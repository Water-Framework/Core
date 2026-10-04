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
import it.water.core.api.bundle.Runtime;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.traffic.TrafficPublisher;
import it.water.core.api.traffic.events.TrafficCallEvent;
import it.water.core.api.traffic.events.TrafficDomainEvent;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.api.traffic.model.TrafficRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static it.water.core.service.traffic.policy.TrafficRecordPolicy.PROP_SAMPLING_RATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TrafficReporterImpl}.
 * <p>
 * Deliberately NOT using {@code WaterTestExtension}/the real {@code ComponentRegistry}: this class
 * has four {@code @Inject} dependencies ({@link ApplicationProperties}, {@link TrafficPublisher},
 * {@link Runtime}, {@link ClusterNodeOptions}) and no lifecycle ({@code @OnActivate}/
 * {@code @OnDeactivate}) methods, so mocking all four directly with Mockito gives full,
 * deterministic branch coverage without any async worker thread or shared test-registry state
 * involved. The end-to-end, DI-resolved wiring (real {@link InMemoryTrafficPublisher} + a
 * registered listener) is separately covered by {@link TrafficReporterEgressHarnessTest}.
 * <p>
 * <b>F4 note.</b> Every {@code report*} method now funnels through
 * {@code TrafficRecordPolicy#normalize}, which rebuilds the record via its Lombok
 * {@code toBuilder()} - the published record is therefore a COPY, never the same reference the
 * caller passed in. Field-by-field {@code assertEquals} (the records carry
 * {@code @EqualsAndHashCode}) is the correct assertion here, not {@code assertSame}; several tests
 * below additionally assert {@code assertNotSame} to make that identity change explicit rather than
 * accidentally re-introducing a same-reference assumption later. {@link Runtime} and
 * {@link ClusterNodeOptions} are left UNSTUBBED in most tests here on purpose: an unstubbed mock
 * behaves exactly like a real, unconfigured collaborator would (every enrichment field it could
 * have filled simply stays null), which is what lets a plain {@code assertEquals(original,
 * normalized)} hold. The full enrichment/redaction/sampling branch matrix is covered in
 * {@code it.water.core.service.traffic.policy.TrafficRecordPolicyTest}.
 */
@ExtendWith(MockitoExtension.class)
class TrafficReporterImplTest {

    private static final String PROP_ENABLED = "water.traffic.enabled";

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private TrafficPublisher trafficPublisher;

    @Mock
    private Runtime waterRuntime;

    @Mock
    private ClusterNodeOptions clusterNodeOptions;

    @InjectMocks
    private TrafficReporterImpl trafficReporter;

    /**
     * Defensive cleanup, not strictly exercised by any test in THIS class: {@code normalize()}
     * enriches {@code correlationId} from {@link TrafficCorrelationContext}, a static ThreadLocal
     * shared by the whole test run on this thread. A scope leaked by another test class (or a
     * future test added here) would otherwise silently break the {@code assertEquals(original,
     * normalized)} assumption below.
     */
    @AfterEach
    void clearCorrelationScope() {
        TrafficCorrelationContext.close();
    }

    private TrafficRecord sampleRecord() {
        return WaterTrafficRecord.builder()
                .recordId("rec-1")
                .recordType(RecordType.API)
                .outcome(Outcome.SUCCESS)
                .build();
    }

    private TrafficCallRecord sampleCallRecord() {
        return WaterTrafficCallRecord.builder()
                .recordId("call-1")
                .recordType(RecordType.REST)
                .operation("GET /foo")
                .build();
    }

    private TrafficDomainEventRecord sampleDomainEventRecord() {
        return WaterTrafficDomainEventRecord.builder()
                .recordId("de-1")
                .recordType(RecordType.PERSISTENCE)
                .build();
    }

    private void stubEnabled(boolean enabled) {
        when(applicationProperties.getPropertyOrDefault(PROP_ENABLED, "false")).thenReturn(String.valueOf(enabled));
    }

    @Test
    void isEnabled_propertyFalse_returnsFalse() {
        stubEnabled(false);
        assertFalse(trafficReporter.isEnabled());
    }

    @Test
    void isEnabled_propertyTrue_returnsTrue() {
        stubEnabled(true);
        assertTrue(trafficReporter.isEnabled());
    }

    @Test
    void report_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.report(sampleRecord());
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportCall_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.reportCall(sampleCallRecord());
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportWithPayload_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.report(sampleRecord(), "ctx");
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportCallWithPayload_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.reportCall(sampleCallRecord(), "ctx");
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void report_enabled_publishesNormalizedCopyEqualToOriginal() {
        stubEnabled(true);
        TrafficRecord record = sampleRecord();

        trafficReporter.report(record);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        // the reporter rebuilds the record (F4 policy normalization) - same field values, different
        // instance; with Runtime/ClusterNodeOptions unstubbed, nothing is actually enriched, so the
        // copy is field-for-field equal to the original
        assertEquals(record, captor.getValue().record());
        assertNotSame(record, captor.getValue().record());
        assertNull(captor.getValue().payload());
        assertNull(captor.getValue().payloadType());
    }

    @Test
    void reportCall_enabled_wrapsCallRecordInCallEventAndPublishes() {
        stubEnabled(true);
        TrafficCallRecord callRecord = sampleCallRecord();

        trafficReporter.reportCall(callRecord);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertEquals(callRecord, captor.getValue().record());
        assertNotSame(callRecord, captor.getValue().record());
        assertTrue(captor.getValue() instanceof TrafficCallEvent);
        assertNull(captor.getValue().payload());
    }

    @Test
    void report_withPayload_enabled_publishesEventWithResolvedPayloadType() {
        stubEnabled(true);
        TrafficRecord record = sampleRecord();

        trafficReporter.report(record, "context-payload");

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertEquals("context-payload", captor.getValue().payload());
        assertEquals(String.class, captor.getValue().payloadType());
    }

    @Test
    void report_withNullPayload_enabled_payloadAndPayloadTypeAreNull() {
        stubEnabled(true);
        TrafficRecord record = sampleRecord();

        trafficReporter.report(record, null);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertNull(captor.getValue().payload());
        assertNull(captor.getValue().payloadType());
    }

    @Test
    void reportCall_withPayload_enabled_publishesEventWithResolvedPayloadType() {
        stubEnabled(true);
        TrafficCallRecord callRecord = sampleCallRecord();

        trafficReporter.reportCall(callRecord, 404);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertEquals(404, captor.getValue().payload());
        assertEquals(Integer.class, captor.getValue().payloadType());
        assertTrue(captor.getValue() instanceof TrafficCallEvent);
    }

    @Test
    void reportDomainEvent_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.reportDomainEvent(sampleDomainEventRecord());
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportDomainEventWithPayload_disabled_shortCircuitsAndDoesNotPublish() {
        stubEnabled(false);
        trafficReporter.reportDomainEvent(sampleDomainEventRecord(), "ctx");
        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportDomainEvent_enabled_publishesNormalizedCopyEqualToOriginal() {
        stubEnabled(true);
        TrafficDomainEventRecord record = sampleDomainEventRecord();

        trafficReporter.reportDomainEvent(record);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertEquals(record, captor.getValue().record());
        assertNotSame(record, captor.getValue().record());
        assertTrue(captor.getValue() instanceof TrafficDomainEvent);
        assertNull(captor.getValue().payload());
        assertNull(captor.getValue().payloadType());
    }

    @Test
    void reportDomainEvent_withPayload_enabled_publishesEventWithResolvedPayloadType() {
        stubEnabled(true);
        TrafficDomainEventRecord record = sampleDomainEventRecord();
        Object payload = new Object();

        trafficReporter.reportDomainEvent(record, payload);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertSame(payload, captor.getValue().payload());
        assertEquals(Object.class, captor.getValue().payloadType());
        assertTrue(captor.getValue() instanceof TrafficDomainEvent);
    }

    @Test
    void reportDomainEvent_withNullPayload_enabled_payloadAndPayloadTypeAreNull() {
        stubEnabled(true);
        TrafficDomainEventRecord record = sampleDomainEventRecord();

        trafficReporter.reportDomainEvent(record, null);

        ArgumentCaptor<TrafficEvent> captor = ArgumentCaptor.forClass(TrafficEvent.class);
        verify(trafficPublisher).publish(captor.capture());
        assertNull(captor.getValue().payload());
        assertNull(captor.getValue().payloadType());
    }

    // ---------------------------------------------------------------------
    // F4: sampling verdict gates publication (full matrix in TrafficRecordPolicyTest)
    // ---------------------------------------------------------------------

    @Test
    void report_enabled_samplingRateZero_doesNotPublish() {
        stubEnabled(true);
        when(applicationProperties.getPropertyOrDefault(PROP_SAMPLING_RATE, null)).thenReturn("0.0");

        trafficReporter.report(sampleRecord());

        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void report_enabled_samplingRateOne_publishes() {
        stubEnabled(true);
        when(applicationProperties.getPropertyOrDefault(PROP_SAMPLING_RATE, null)).thenReturn("1.0");

        trafficReporter.report(sampleRecord());

        verify(trafficPublisher).publish(any());
    }

    // ---------------------------------------------------------------------
    // Null record: never published, never throws
    // ---------------------------------------------------------------------

    @Test
    void report_nullRecord_doesNotPublishAndDoesNotThrow() {
        // isEnabled() is short-circuited away by "record == null" BEFORE it is ever checked - no
        // stubEnabled() call here, or it would be an unused stub under Mockito's strict stubs
        assertDoesNotThrow(() -> trafficReporter.report(null));

        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportCall_nullRecord_doesNotPublishAndDoesNotThrow() {
        assertDoesNotThrow(() -> trafficReporter.reportCall(null));

        verifyNoInteractions(trafficPublisher);
    }

    @Test
    void reportDomainEvent_nullRecord_doesNotPublishAndDoesNotThrow() {
        assertDoesNotThrow(() -> trafficReporter.reportDomainEvent(null));

        verifyNoInteractions(trafficPublisher);
    }

    // ---------------------------------------------------------------------
    // F5: counters of the choke point. They exist to answer "is nothing arriving, or is something
    // dropping it?" - a question a silent pipeline always raises and that logs alone cannot settle.
    // ---------------------------------------------------------------------

    @Test
    void counters_startAtZero() {
        assertEquals(0, trafficReporter.recordsReceived());
        assertEquals(0, trafficReporter.recordsReported());
        assertEquals(0, trafficReporter.recordsSampledOut());
        assertEquals(0, trafficReporter.recordsFailed());
    }

    @Test
    void reportedRecord_countsAsReceivedAndReported() {
        stubEnabled(true);

        trafficReporter.reportCall(sampleCallRecord());

        assertEquals(1, trafficReporter.recordsReceived());
        assertEquals(1, trafficReporter.recordsReported());
        assertEquals(0, trafficReporter.recordsSampledOut());
        assertEquals(0, trafficReporter.recordsFailed());
    }

    /**
     * With the master switch off nothing is counted at all: the reporter is inert, and counting there
     * would turn "disabled" into a stream of numbers suggesting activity.
     */
    @Test
    void disabledReporter_countsNothing() {
        stubEnabled(false);

        trafficReporter.reportCall(sampleCallRecord());

        assertEquals(0, trafficReporter.recordsReceived());
        assertEquals(0, trafficReporter.recordsReported());
    }

    @Test
    void sampledOutRecord_countsAsReceivedButNotReported() {
        // Stubbed through a property MAP rather than per-key `when(...)` calls: the numeric properties
        // are read with a null default while the flags use "false"/"", and getPropertyOrDefault is an
        // overloaded default method - so per-key stubbing has to reproduce each default exactly or it
        // silently does not match, which is how this test first passed while sampling nothing out.
        stubProperties(Map.of(PROP_ENABLED, "true", PROP_SAMPLING_RATE, "0.0"));

        trafficReporter.reportCall(sampleCallRecord());

        assertEquals(1, trafficReporter.recordsReceived());
        assertEquals(0, trafficReporter.recordsReported());
        assertEquals(1, trafficReporter.recordsSampledOut());
        verifyNoInteractions(trafficPublisher);
    }

    /**
     * Answers every property lookup from a map, falling back to the default the caller asked for.
     * Mirrors how a real {@code ApplicationProperties} behaves, so a test does not have to know which
     * default each call site passes.
     */
    private void stubProperties(Map<String, String> values) {
        when(applicationProperties.getPropertyOrDefault(anyString(), nullable(String.class)))
                .thenAnswer(invocation -> values.getOrDefault(
                        invocation.getArgument(0), invocation.getArgument(1)));
    }

    /**
     * A publisher that blows up must not surface in the caller - it is wrapped around someone else's
     * business call - but the loss has to be visible somewhere, and this counter is that somewhere.
     */
    @Test
    void publisherFailure_isSwallowedAndCounted() {
        stubEnabled(true);
        doThrow(new IllegalStateException("egress down")).when(trafficPublisher).publish(any());

        assertDoesNotThrow(() -> trafficReporter.reportCall(sampleCallRecord()));

        assertEquals(1, trafficReporter.recordsReceived());
        assertEquals(0, trafficReporter.recordsReported());
        assertEquals(1, trafficReporter.recordsFailed());
    }
}
