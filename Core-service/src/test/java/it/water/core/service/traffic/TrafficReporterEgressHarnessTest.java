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

import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.api.traffic.TrafficPublisher;
import it.water.core.api.traffic.TrafficPublisherStats;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.events.TrafficCallEvent;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.ComponentConfigurationFactory;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Real, DI-resolved end-to-end harness test for the "egress in-memory" slice: a real
 * {@link TrafficReporterImpl} (Api entry point), delegating to the real, framework-activated
 * {@link InMemoryTrafficPublisher}, dispatching to a test {@link ApplicationEventListener}
 * registered directly in the real {@code ComponentRegistry} (see {@link TestTrafficEventListener}).
 * <p>
 * Dispatch is asynchronous (single daemon worker thread draining a queue), so every assertion that
 * depends on a dispatched event uses {@link #awaitCapture(String)}, a bounded poll-with-timeout
 * helper, rather than an immediate assertion right after calling {@code report(...)}.
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrafficReporterEgressHarnessTest implements Service {

    private static final long POLL_TIMEOUT_MS = 3000L;
    private static final long POLL_INTERVAL_MS = 50L;
    private static final String PROP_ENABLED = "water.traffic.enabled";

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Inject
    @Setter
    private TrafficReporter trafficReporter;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeAll
    void initializeTestFramework() {
        TestTrafficEventListener.reset();
        // IMPORTANT: register/resolve everything through the SAME componentRegistry instance that was
        // @Inject-ed into THIS test (i.e. the one TrafficReporterImpl/InMemoryTrafficPublisher actually
        // resolve their own dependencies from). Going through TestRuntimeInitializer.getInstance() directly
        // is NOT guaranteed to be equivalent: TestRuntimeInitializer.start() re-runs framework component
        // scanning/registration on every WaterTestExtension#beforeAll invocation across the module's test
        // run, which re-creates and re-registers a FRESH ApplicationProperties each time while
        // TestRuntimeInitializer's own "waterApplicationProperties" field is reassigned to the LATEST one -
        // whereas ComponentRegistry#findComponent (used by the real @Inject wiring) keeps returning the
        // FIRST-ever-registered instance for a given priority (stable insertion-order tie-break). Using
        // getInstance() for the override caused it to silently mutate an orphaned, unused
        // ApplicationProperties instance, so TrafficReporterImpl.isEnabled() never observed it.
        componentRegistry.registerComponent(
                ApplicationEventListener.class,
                new TestTrafficEventListener(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        setTrafficEnabled(false);
    }

    @AfterAll
    void tearDown() {
        setTrafficEnabled(false);
    }

    private void setTrafficEnabled(boolean enabled) {
        TestApplicationProperties props = (TestApplicationProperties) componentRegistry.findComponent(ApplicationProperties.class, null);
        props.override(PROP_ENABLED, String.valueOf(enabled));
    }

    private WaterTrafficRecord.WaterTrafficRecordBuilder<?, ?> baseRecordBuilder(String marker) {
        return WaterTrafficRecord.builder()
                .recordId(marker)
                .recordType(RecordType.API)
                .timestamp(Instant.now())
                .serviceName("core-service-test")
                .moduleId("Core-service")
                .nodeId("node-test")
                .outcome(Outcome.SUCCESS)
                .durationMillis(1L);
    }

    private WaterTrafficCallRecord.WaterTrafficCallRecordBuilder<?, ?> baseCallRecordBuilder(String marker) {
        return WaterTrafficCallRecord.builder()
                .recordId(marker)
                .recordType(RecordType.REST)
                .timestamp(Instant.now())
                .serviceName("core-service-test")
                .moduleId("Core-service")
                .nodeId("node-test")
                .outcome(Outcome.SUCCESS)
                .durationMillis(1L)
                .source("test-client")
                .destination("test-service")
                .operation("GET /test")
                .httpMethod("GET")
                .path("/test")
                .statusCode(200)
                .clientIp("127.0.0.1");
    }

    private TestTrafficEventListener.Captured awaitCapture(String marker) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Optional<TestTrafficEventListener.Captured> found = TestTrafficEventListener.CAPTURED.stream()
                    .filter(c -> marker.equals(c.record.recordId()))
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for async traffic dispatch");
            }
        }
        fail("Timed out waiting for traffic event [" + marker + "] to reach the test listener");
        return null;
    }

    private void assertNeverCaptured(String marker) {
        try {
            Thread.sleep(300L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(TestTrafficEventListener.CAPTURED.stream().noneMatch(c -> marker.equals(c.record.recordId())),
                "Traffic event [" + marker + "] should NOT have been dispatched while reporting is disabled");
    }

    @Test
    @Order(1)
    void isEnabled_explicitlyDisabled_returnsFalse() {
        setTrafficEnabled(false);
        assertFalse(trafficReporter.isEnabled());
    }

    @Test
    @Order(2)
    void report_disabled_doesNotNotifyListener() {
        setTrafficEnabled(false);
        String marker = "disabled-report-" + UUID.randomUUID();
        trafficReporter.report(baseRecordBuilder(marker).build());
        assertNeverCaptured(marker);
    }

    @Test
    @Order(3)
    void reportCall_disabled_doesNotNotifyListener() {
        setTrafficEnabled(false);
        String marker = "disabled-call-" + UUID.randomUUID();
        trafficReporter.reportCall(baseCallRecordBuilder(marker).build());
        assertNeverCaptured(marker);
    }

    @Test
    @Order(4)
    void report_enabled_dispatchesRecordToRegisteredListener() {
        setTrafficEnabled(true);
        assertTrue(trafficReporter.isEnabled());
        String marker = "enabled-report-" + UUID.randomUUID();

        trafficReporter.report(baseRecordBuilder(marker).build());

        TestTrafficEventListener.Captured captured = awaitCapture(marker);
        assertEquals(marker, captured.record.recordId());
        assertTrue(captured.event instanceof TrafficEvent);
        TrafficEvent<?> event = (TrafficEvent<?>) captured.event;
        assertNull(event.payload());
        assertNull(event.payloadType());
    }

    @Test
    @Order(5)
    void reportCall_enabled_dispatchesCallRecordToRegisteredListener() {
        setTrafficEnabled(true);
        String marker = "enabled-call-" + UUID.randomUUID();

        trafficReporter.reportCall(baseCallRecordBuilder(marker).build());

        TestTrafficEventListener.Captured captured = awaitCapture(marker);
        assertTrue(captured.event instanceof TrafficCallEvent);
        assertTrue(captured.record instanceof TrafficCallRecord);
        assertEquals("/test", ((TrafficCallRecord) captured.record).path());
    }

    @Test
    @Order(6)
    void report_withPayload_enabled_dispatchesEventWithPayload() {
        setTrafficEnabled(true);
        String marker = "payload-report-" + UUID.randomUUID();

        trafficReporter.report(baseRecordBuilder(marker).build(), "context-payload");

        TestTrafficEventListener.Captured captured = awaitCapture(marker);
        TrafficEvent<?> event = (TrafficEvent<?>) captured.event;
        assertEquals("context-payload", event.payload());
        assertEquals(String.class, event.payloadType());
    }

    @Test
    @Order(7)
    void reportCall_withPayload_enabled_dispatchesEventWithPayload() {
        setTrafficEnabled(true);
        String marker = "payload-call-" + UUID.randomUUID();

        trafficReporter.reportCall(baseCallRecordBuilder(marker).build(), 404);

        TestTrafficEventListener.Captured captured = awaitCapture(marker);
        TrafficEvent<?> event = (TrafficEvent<?>) captured.event;
        assertEquals(404, event.payload());
        assertEquals(Integer.class, event.payloadType());
    }

    @Test
    @Order(8)
    void publisherStats_afterSuccessfulPublish_reflectExpectedValues() {
        setTrafficEnabled(true);
        TrafficPublisherStats stats = componentRegistry.findComponent(TrafficPublisherStats.class, null);
        TrafficPublisher publisher = componentRegistry.findComponent(TrafficPublisher.class, null);
        assertNotNull(stats);
        assertNotNull(publisher);

        long enqueuedBefore = stats.enqueued();
        long publishedBefore = stats.published();
        String marker = "stats-" + UUID.randomUUID();

        trafficReporter.report(baseRecordBuilder(marker).build());
        awaitCapture(marker);

        assertTrue(stats.enqueued() > enqueuedBefore, "enqueued() should have increased after a successful publish");
        assertTrue(stats.published() > publishedBefore, "published() should have increased once dispatch completes");
        assertEquals("memory", publisher.publisherType());
        assertTrue(publisher.isAvailable());
        // InMemoryTrafficPublisher now lazily starts its worker on the FIRST publish() call (a proxied,
        // per-call-injected invocation) rather than on @OnActivate (which used to run on the raw,
        // un-proxied instance before applicationProperties was populated). By this point in the test
        // several earlier scenarios have already called report()/reportCall() while enabled, so the
        // worker has long since started with applicationProperties correctly injected - queueCapacity()
        // can now be asserted deterministically against the test property.
        assertEquals(500, stats.queueCapacity());
    }
}
