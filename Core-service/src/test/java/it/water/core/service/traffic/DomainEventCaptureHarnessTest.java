/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.core.service.traffic;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.entity.events.PostSaveEvent;
import it.water.core.api.entity.events.PostUpdateDetailedEvent;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.ApplicationEventProducer;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.service.traffic.events.TrafficDomainEventListener;
import it.water.core.testing.utils.bundle.TestApplicationProperties;
import it.water.core.testing.utils.junit.WaterTestExtension;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Real, DI-resolved end-to-end harness test that closes the F2 loop: a genuine
 * {@link ApplicationEventProducer} ({@code InMemoryApplicationEventProducer}, resolved from the
 * real, framework-scanned {@link ComponentRegistry}) emits a domain event, which the already
 * {@code @FrameworkComponent}-registered {@code TrafficDomainEventListener} observes, normalizes and
 * hands to the real {@code TrafficReporterImpl}, which in turn delegates to the real
 * {@code InMemoryTrafficPublisher} - whose (asynchronous) egress finally reaches the reused
 * {@link TestTrafficEventListener}, exactly as in {@link TrafficReporterEgressHarnessTest}.
 * <p>
 * Chain under test: {@code producer.produceEvent(entity, PostSaveEvent.class)} &rarr;
 * {@code TrafficDomainEventListener} &rarr; {@code TrafficReporterImpl} &rarr;
 * {@code InMemoryTrafficPublisher} &rarr; {@link TestTrafficEventListener} captures a
 * {@link TrafficDomainEventRecord}.
 * <p>
 * Reuses the same gotcha-fix as {@code TrafficReporterEgressHarnessTest}/{@code S2STrafficCaptureHarnessTest}:
 * {@link ApplicationProperties} (and here, the {@link ApplicationEventProducer} itself) are ALWAYS
 * resolved through the test's own {@code @Inject}-ed {@link ComponentRegistry}, never through
 * {@code TestRuntimeInitializer.getInstance()} directly.
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DomainEventCaptureHarnessTest implements Service {

    private static final long POLL_TIMEOUT_MS = 3000L;
    private static final long POLL_INTERVAL_MS = 50L;

    private static final String PROP_TRAFFIC_ENABLED = "water.traffic.enabled";
    private static final String PROP_EVENTS_ENABLED = "water.traffic.events.enabled";
    private static final String PROP_EVENTS_MODE = "water.traffic.events.mode";
    private static final String PROP_EVENTS_WHITELIST = "water.traffic.events.whitelist";

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    private ApplicationEventProducer producer;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeAll
    void initializeTestFramework() {
        TestTrafficEventListener.reset();
        componentRegistry.registerComponent(
                ApplicationEventListener.class,
                new TestTrafficEventListener(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        // resolved through the SAME componentRegistry instance @Inject-ed into THIS test, for the same
        // reason documented on TrafficReporterEgressHarnessTest#initializeTestFramework
        producer = componentRegistry.findComponent(ApplicationEventProducer.class, null);
        resetProperties();
    }

    @AfterAll
    void tearDown() {
        resetProperties();
    }

    private void resetProperties() {
        setTrafficEnabled(false);
        setEventsEnabled(false);
        setEventsMode("opt-in");
        setWhitelist("");
    }

    private TestApplicationProperties props() {
        return (TestApplicationProperties) componentRegistry.findComponent(ApplicationProperties.class, null);
    }

    private void setTrafficEnabled(boolean enabled) {
        props().override(PROP_TRAFFIC_ENABLED, String.valueOf(enabled));
    }

    private void setEventsEnabled(boolean enabled) {
        props().override(PROP_EVENTS_ENABLED, String.valueOf(enabled));
    }

    private void setEventsMode(String mode) {
        props().override(PROP_EVENTS_MODE, mode);
    }

    private void setWhitelist(String whitelist) {
        props().override(PROP_EVENTS_WHITELIST, whitelist);
    }

    private long nextId() {
        return ID_SEQ.incrementAndGet();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * How many captures a single emitted domain event legitimately produces, i.e. the fan-out
     * factor of the pipeline in THIS JVM:
     * <pre>  (# TrafficDomainEventListener) x (# TestTrafficEventListener)  </pre>
     * The producer delivers the event to every registered domain listener, each of them reports it
     * (one publish each), and the publisher in turn fans every published record out to every
     * registered listener.
     * <p>
     * Neither factor is reliably one: the registry accumulates across the harness classes of this
     * module within the same test JVM - each class registers its own {@link TestTrafficEventListener}
     * and never unregisters it, and {@code TestRuntimeInitializer.start()} re-scans and re-registers
     * the {@code @FrameworkComponent}s (hence the domain listener) on every {@code @BeforeAll}. This
     * is a test-runtime artifact, NOT a duplicate-delivery bug of the pipeline; computing the factor
     * instead of hard-coding it is what keeps this assertion both exact and stable.
     */
    @SuppressWarnings("rawtypes")
    private long expectedCaptures() {
        List<ApplicationEventListener> listeners =
                componentRegistry.findComponents(ApplicationEventListener.class, null);
        long domainListeners = listeners.stream().filter(TrafficDomainEventListener.class::isInstance).count();
        long testListeners = listeners.stream().filter(TestTrafficEventListener.class::isInstance).count();
        return domainListeners * testListeners;
    }

    private long countDomainRecordsForResourceId(String resourceId) {
        return TestTrafficEventListener.CAPTURED.stream()
                .filter(c -> c.record instanceof TrafficDomainEventRecord)
                .filter(c -> resourceId.equals(((TrafficDomainEventRecord) c.record).resourceId()))
                .count();
    }

    private TrafficDomainEventRecord awaitDomainRecord(String resourceId) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Optional<TestTrafficEventListener.Captured> found = TestTrafficEventListener.CAPTURED.stream()
                    .filter(c -> c.record instanceof TrafficDomainEventRecord)
                    .filter(c -> resourceId.equals(((TrafficDomainEventRecord) c.record).resourceId()))
                    .findFirst();
            if (found.isPresent()) {
                return (TrafficDomainEventRecord) found.get().record;
            }
            sleep(POLL_INTERVAL_MS);
        }
        fail("Timed out waiting for a TrafficDomainEventRecord with resourceId [" + resourceId + "]");
        return null;
    }

    private void assertDomainRecordNeverCaptured(String resourceId) {
        sleep(300L);
        assertEquals(0, countDomainRecordsForResourceId(resourceId),
                "resourceId [" + resourceId + "] should NOT have produced a TrafficDomainEventRecord");
    }

    @Test
    @Order(1)
    void produceEvent_postSaveEvent_onWhitelistedEntity_capturesPersistenceRecord() {
        TestTrafficEventListener.reset();
        setTrafficEnabled(true);
        setEventsEnabled(true);
        setEventsMode("opt-in");
        setWhitelist(DomainEventCaptureTestEntity.class.getName());
        long id = nextId();
        DomainEventCaptureTestEntity entity = new DomainEventCaptureTestEntity(id);

        producer.produceEvent(entity, PostSaveEvent.class);

        TrafficDomainEventRecord record = awaitDomainRecord(String.valueOf(id));
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.SAVE, record.changeOperation());
        assertEquals(DomainEventCaptureTestEntity.class.getName(), record.resourceType());
        assertEquals(String.valueOf(id), record.resourceId());
        // the eventClass is the Water INTERFACE, not the synthetic JDK proxy class name
        assertEquals(PostSaveEvent.class.getName(), record.eventClass());
    }

    @Test
    @Order(2)
    void produceDetailedEvent_postUpdateDetailedEvent_capturesUpdateOperationAndBeforeAfterRefs() {
        TestTrafficEventListener.reset();
        setTrafficEnabled(true);
        setEventsEnabled(true);
        setEventsMode("opt-in");
        setWhitelist(DomainEventCaptureTestEntity.class.getName());
        long beforeId = nextId();
        long afterId = nextId();
        DomainEventCaptureTestEntity before = new DomainEventCaptureTestEntity(beforeId);
        DomainEventCaptureTestEntity after = new DomainEventCaptureTestEntity(afterId);

        producer.produceDetailedEvent(before, after, PostUpdateDetailedEvent.class);

        TrafficDomainEventRecord record = awaitDomainRecord(String.valueOf(afterId));
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.UPDATE, record.changeOperation());
        assertEquals(DomainEventCaptureTestEntity.class.getName() + "#" + beforeId, record.beforeRef());
        assertEquals(DomainEventCaptureTestEntity.class.getName() + "#" + afterId, record.afterRef());
    }

    @Test
    @Order(3)
    void produceEvent_entityNotInWhitelist_optIn_noRecordCaptured() {
        TestTrafficEventListener.reset();
        setTrafficEnabled(true);
        setEventsEnabled(true);
        setEventsMode("opt-in");
        setWhitelist("some.other.NotWhitelistedEntity");
        long id = nextId();
        DomainEventCaptureTestEntity entity = new DomainEventCaptureTestEntity(id);

        producer.produceEvent(entity, PostSaveEvent.class);

        assertDomainRecordNeverCaptured(String.valueOf(id));
    }

    @Test
    @Order(4)
    void produceEvent_trafficGloballyDisabled_noRecordCaptured() {
        TestTrafficEventListener.reset();
        setTrafficEnabled(false);
        setEventsEnabled(true);
        setEventsMode("opt-in");
        setWhitelist(DomainEventCaptureTestEntity.class.getName());
        long id = nextId();
        DomainEventCaptureTestEntity entity = new DomainEventCaptureTestEntity(id);

        producer.produceEvent(entity, PostSaveEvent.class);

        assertDomainRecordNeverCaptured(String.valueOf(id));
    }

    /**
     * The most important scenario in this class: {@code InMemoryTrafficPublisher} dispatches every
     * published {@link it.water.core.api.traffic.events.TrafficEvent} back to EVERY registered
     * {@code ApplicationEventListener} - {@code TrafficDomainEventListener} included. Without its
     * loop guard (subject-is-a-{@code TrafficRecord} / event-is-a-{@code TrafficEvent}) the pipeline
     * would re-observe and re-report its own output, and the number of captured records for this
     * {@code resourceId} would keep GROWING on every additional wait.
     * <p>
     * Deliberately NOT asserting an exact count of {@code 1}: the module's {@code ComponentRegistry}
     * is shared/accumulates across this test run, so sibling harness classes
     * ({@code TrafficReporterEgressHarnessTest}, {@code S2STrafficCaptureHarnessTest}) may each have
     * registered their own {@link TestTrafficEventListener} instance in their own {@code @BeforeAll} -
     * every instance shares the same static {@code CAPTURED} list, so a single dispatch can
     * legitimately be observed more than once. That duplication is harmless and stable; a genuine
     * loop-guard failure is not - it produces additional, NEW records over time. The real proof is
     * the STABILITY of the count across the second wait, not its absolute value.
     */
    @Test
    @Order(5)
    void produceEvent_loopGuardHolds_recordCountForResourceIdStaysStableOverTime() {
        TestTrafficEventListener.reset();
        setTrafficEnabled(true);
        setEventsEnabled(true);
        setEventsMode("opt-in");
        setWhitelist(DomainEventCaptureTestEntity.class.getName());
        long id = nextId();
        DomainEventCaptureTestEntity entity = new DomainEventCaptureTestEntity(id);

        producer.produceEvent(entity, PostSaveEvent.class);

        awaitDomainRecord(String.valueOf(id));
        // let the whole fan-out settle before counting: ONE published record is delivered to EVERY
        // registered TestTrafficEventListener (sibling harness classes in this JVM each registered
        // their own against the same shared CAPTURED list), and those deliveries land a few
        // microseconds apart - sampling right after the first one would race with the others
        sleep(500L);
        long settledCount = countDomainRecordsForResourceId(String.valueOf(id));
        assertEquals(expectedCaptures(), settledCount,
                "each domain listener must have reported the event EXACTLY ONCE: a broken loop guard "
                        + "would re-report every record it receives back from the publisher");

        sleep(500L);
        assertEquals(settledCount, countDomainRecordsForResourceId(String.valueOf(id)),
                "record count must remain stable over time - the loop guard must prevent "
                        + "TrafficDomainEventListener from re-observing/re-reporting its own output "
                        + "(a guard failure would make this count keep growing)");
    }
}
