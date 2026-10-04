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
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficCallRecord;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Real, DI-resolved end-to-end harness test for the Service-to-Service (S2S) traffic capture slice
 * (Fase 3): a genuine {@code @ReportTraffic}-annotated test Service, registered and resolved
 * PROXIED from the real {@code ComponentRegistry} (so the invocation actually goes through
 * {@code TestServiceProxy#invoke} - before &rarr; invoke &rarr; after, exactly like the real
 * runtime), dispatching a {@link TrafficCallRecord} to the reused {@link TestTrafficEventListener}.
 * <p>
 * Reuses the same gotcha-fix as {@code TrafficReporterEgressHarnessTest}: {@link ApplicationProperties}
 * is ALWAYS resolved through the test's own {@code @Inject}-ed {@link ComponentRegistry}, never
 * through {@code TestRuntimeInitializer.getInstance()} directly (see that class's Javadoc for why).
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S2STrafficCaptureHarnessTest implements Service {

    private static final long POLL_TIMEOUT_MS = 3000L;
    private static final long POLL_INTERVAL_MS = 50L;
    private static final String PROP_ENABLED = "water.traffic.enabled";
    /**
     * The S2S capture is opt-out now: this second switch is what turns it on for the whole runtime,
     * and no annotation is needed on the intercepted methods any more.
     */
    private static final String PROP_S2S_ENABLED = "water.traffic.s2s.enabled";

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    private TestS2SApi s2sApi;
    private TestS2SSystemApi s2sSystemApi;
    private TestPlainService plainService;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeAll
    void initializeTestFramework() {
        TestTrafficEventListener.reset();
        componentRegistry.registerComponent(
                ApplicationEventListener.class,
                new TestTrafficEventListener(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        componentRegistry.registerComponent(
                TestS2SApi.class,
                new TestS2SServiceImpl(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        componentRegistry.registerComponent(
                TestS2SSystemApi.class,
                new TestS2SSystemServiceImpl(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        componentRegistry.registerComponent(
                TestPlainService.class,
                new TestPlainServiceImpl(),
                ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
        // resolve the PROXIED instances: invoking through these triggers TestServiceProxy#invoke,
        // which runs the Before/After traffic interceptors exactly as any real Water runtime would
        s2sApi = componentRegistry.findComponent(TestS2SApi.class, null);
        s2sSystemApi = componentRegistry.findComponent(TestS2SSystemApi.class, null);
        plainService = componentRegistry.findComponent(TestPlainService.class, null);
        setTrafficEnabled(false);
    }

    @AfterAll
    void tearDown() {
        setTrafficEnabled(false);
    }

    private void setTrafficEnabled(boolean enabled) {
        TestApplicationProperties props =
                (TestApplicationProperties) componentRegistry.findComponent(ApplicationProperties.class, null);
        props.override(PROP_ENABLED, String.valueOf(enabled));
        // both switches move together here: the master one gates the reporter, the S2S one gates the
        // global interceptor that feeds it
        props.override(PROP_S2S_ENABLED, String.valueOf(enabled));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private TestTrafficEventListener.Captured awaitCaptureByOperation(String operation) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Optional<TestTrafficEventListener.Captured> found = TestTrafficEventListener.CAPTURED.stream()
                    .filter(c -> c.record instanceof TrafficCallRecord)
                    .filter(c -> operation.equals(((TrafficCallRecord) c.record).operation()))
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            sleep(POLL_INTERVAL_MS);
        }
        fail("Timed out waiting for a TrafficCallRecord with operation [" + operation + "]");
        return null;
    }

    private void assertOperationNeverCaptured(String operation) {
        sleep(300L);
        assertTrue(TestTrafficEventListener.CAPTURED.stream()
                        .filter(c -> c.record instanceof TrafficCallRecord)
                        .noneMatch(c -> operation.equals(((TrafficCallRecord) c.record).operation())),
                "operation [" + operation + "] should NOT have produced a traffic record");
    }

    /**
     * One logical call must produce ONE record, no matter how many times the registry hands back the
     * interceptor.
     * <p>
     * This is a regression test for a real defect, not a theoretical one: a global interceptor declares
     * two services (before and after) and is therefore registered - and returned - once per
     * registration, so it used to be invoked three times per call. The result was three nested spans
     * per hop, each pointing at the previous one as its parent: a plausible-looking call tree that was
     * the same hop reported three times, with inflated depths. The assertion is on DISTINCT recordIds
     * because the same record legitimately reaches several registered listeners.
     */
    @Test
    @Order(0)
    void oneCall_producesExactlyOneRecord_regardlessOfDuplicateRegistrations() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        s2sApi.doWork("single");
        awaitCaptureByOperation("doWork");

        long distinctRecords = TestTrafficEventListener.CAPTURED.stream()
                .filter(c -> c.record instanceof TrafficCallRecord)
                .map(c -> (TrafficCallRecord) c.record)
                .filter(r -> "doWork".equals(r.operation()))
                .map(TrafficCallRecord::recordId)
                .distinct()
                .count();
        assertEquals(1, distinctRecords, "a single invocation must yield a single traffic record");
    }

    @Test
    @Order(1)
    void doWork_enabled_dispatchesCallRecordWithExpectedFields() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = s2sApi.doWork("payload-1");

        assertEquals("handled:payload-1", result);
        TestTrafficEventListener.Captured captured = awaitCaptureByOperation("doWork");
        TrafficCallRecord record = (TrafficCallRecord) captured.record;
        assertEquals(Outcome.SUCCESS, record.outcome());
        assertEquals("doWork", record.operation());
        assertEquals(TestS2SServiceImpl.class.getName(), record.destination());
        assertTrue(record.durationMillis() >= 0);
        assertNotNull(record.correlationId());
        assertEquals(RecordType.API, record.recordType());
    }

    @Test
    @Order(2)
    void doWorkDefaultOperation_enabled_operationFallsBackToMethodName() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = s2sApi.doWorkDefaultOperation("x");

        assertEquals("handled-default:x", result);
        TestTrafficEventListener.Captured captured = awaitCaptureByOperation("doWorkDefaultOperation");
        assertEquals("doWorkDefaultOperation", ((TrafficCallRecord) captured.record).operation());
    }

    @Test
    @Order(3)
    void doSystemWork_enabled_recordTypeIsSystemApi() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        s2sSystemApi.doSystemWork("y");

        TestTrafficEventListener.Captured captured = awaitCaptureByOperation("doSystemWork");
        TrafficCallRecord record = (TrafficCallRecord) captured.record;
        assertEquals(RecordType.SYSTEM_API, record.recordType());
        assertEquals(TestS2SSystemServiceImpl.class.getName(), record.destination());
    }

    /**
     * The point of the opt-out design: a method carrying NO annotation at all is captured anyway.
     * Under the previous opt-in capture this call would have produced nothing.
     */
    @Test
    @Order(31)
    void doPlainWork_notAnnotated_isCapturedAnyway() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = s2sApi.doPlainWork("z");

        assertEquals("handled-plain:z", result);
        TrafficCallRecord record = (TrafficCallRecord) awaitCaptureByOperation("doPlainWork").record;
        assertEquals(RecordType.API, record.recordType());
        assertEquals(TestS2SServiceImpl.class.getName(), record.destination());
    }

    @Test
    @Order(32)
    void doSilentWork_annotatedNoTraffic_isExcluded() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = s2sApi.doSilentWork("q");

        assertEquals("handled-silent:q", result);
        assertOperationNeverCaptured("doSilentWork");
    }

    /**
     * Call tree: the inner SystemApi call must point at the outer call as its parent, sit one level
     * deeper, and share its correlation - otherwise the records are just siblings in a bag and the
     * flow cannot be reconstructed, which is the reason parentId/depth exist.
     */
    @Test
    @Order(33)
    void doNestedWork_innerCallPointsAtOuterCallAsParent() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = s2sApi.doNestedWork("nested");

        assertEquals("handled-nested:system-handled:nested", result);
        awaitCaptureByOperation("doSystemWork");

        // The assertion is on the LINKED PAIR, not on two records picked independently, because a
        // single logical call does not produce a single record here: in this test runtime the service
        // is reached through nested proxies, so each hop is intercepted once per layer (and the same
        // record reaches several listeners registered on the shared registry). Absolute depths are
        // therefore meaningless - the invariant that must hold, and the whole reason parentId exists,
        // is that the inner call points at the caller it actually ran inside, one level deeper.
        List<TrafficCallRecord> calls = TestTrafficEventListener.CAPTURED.stream()
                .filter(c -> c.record instanceof TrafficCallRecord)
                .map(c -> (TrafficCallRecord) c.record)
                .toList();
        List<TrafficCallRecord> outerCalls = calls.stream()
                .filter(r -> "doNestedWork".equals(r.operation())).toList();
        assertTrue(!outerCalls.isEmpty(), "the outer call must have been captured");

        TrafficCallRecord linkedInner = calls.stream()
                .filter(r -> "doSystemWork".equals(r.operation()))
                .filter(r -> outerCalls.stream().anyMatch(o -> o.recordId().equals(r.parentId())))
                .findFirst()
                .orElse(null);
        assertNotNull(linkedInner, "no doSystemWork record points at a doNestedWork record as its parent");

        TrafficCallRecord parent = outerCalls.stream()
                .filter(o -> o.recordId().equals(linkedInner.parentId()))
                .findFirst()
                .orElseThrow();
        assertEquals(parent.depth() + 1, linkedInner.depth(), "the nested call sits one level deeper than its parent");
        assertEquals(parent.correlationId(), linkedInner.correlationId(), "parent and child share the correlation");
    }

    /**
     * R3 prevented, not just bounded: since the error hook pops the span the entry hook pushed, a call
     * that throws no longer leaves an orphan span behind - so the NEXT successful call is not adopted by
     * a stale parent. Asserted through the observable consequence: the depth of a call made right after
     * a failure equals the depth of the same call made before it.
     */
    @Test
    @Order(35)
    void failedCall_doesNotLeaveAnOrphanSpan_nextCallKeepsItsDepth() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        s2sApi.doPlainWork("before-failure");
        int depthBefore = ((TrafficCallRecord) awaitCaptureByOperation("doPlainWork").record).depth();

        assertThrows(IllegalStateException.class, () -> s2sApi.doWorkThrows("leak-check"));
        awaitCaptureByOperation("doWorkThrows");

        TestTrafficEventListener.reset();
        s2sApi.doPlainWork("after-failure");
        int depthAfter = ((TrafficCallRecord) awaitCaptureByOperation("doPlainWork").record).depth();

        assertEquals(depthBefore, depthAfter, "a failed call must not deepen the spans of later calls");
    }

    /**
     * The scope is the four architectural layers, not "everything proxied". A plain {@code Service} is
     * intercepted exactly like the others, and must still produce nothing - which is also what keeps
     * the framework's plumbing and the traffic pipeline out of the telemetry (they too are plain
     * Services), with no exclusion list and no re-entrancy guard involved.
     */
    @Test
    @Order(34)
    void plainService_notOnATracedLayer_isIgnored() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        String result = plainService.doInternalWork("guard");
        assertEquals("internal:guard", result);
        // anchor: a call that IS on a traced layer, so the assertions below cannot pass just because
        // capture happened to be off
        s2sApi.doPlainWork("anchor");
        awaitCaptureByOperation("doPlainWork");

        assertOperationNeverCaptured("doInternalWork");
        // the pipeline's own services are plain Services too: reporting is never itself traffic
        assertOperationNeverCaptured("publish");
        assertOperationNeverCaptured("reportCall");
        assertOperationNeverCaptured("isEnabled");
        // and neither is the interception machinery, whose interceptors are Services as well
        assertOperationNeverCaptured("getAnnotation");
    }

    @Test
    @Order(4)
    void doWork_disabled_noRecordDispatched() {
        setTrafficEnabled(false);
        TestTrafficEventListener.reset();

        String result = s2sApi.doWork("payload-disabled");

        assertEquals("handled:payload-disabled", result);
        assertOperationNeverCaptured("doWork");
    }

    /**
     * R1 CLOSED: the failure path is now captured. Was the documented limitation of the original
     * design - the after-hook never ran on an exception, so exactly the calls worth investigating were
     * the invisible ones - and is now covered by the {@code interceptError} hook the runtimes call
     * before rethrowing.
     */
    @Test
    @Order(5)
    void doWorkThrows_enabled_exceptionPropagates_andErrorRecordIsProduced() {
        setTrafficEnabled(true);
        TestTrafficEventListener.reset();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> s2sApi.doWorkThrows("boom"));
        assertTrue(ex.getMessage().contains("boom"), "the business exception must reach the caller unchanged");

        TrafficCallRecord record = (TrafficCallRecord) awaitCaptureByOperation("doWorkThrows").record;
        assertEquals(Outcome.ERROR, record.outcome());
        assertEquals(IllegalStateException.class.getName(), record.errorType());
        assertTrue(record.errorMessage().contains("boom"), "the failure reason must be on the record");
        assertEquals(TestS2SServiceImpl.class.getName(), record.destination());
    }
}
