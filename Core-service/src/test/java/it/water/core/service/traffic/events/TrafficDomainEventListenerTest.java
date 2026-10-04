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
package it.water.core.service.traffic.events;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.entity.events.PostSaveEvent;
import it.water.core.api.entity.events.PreSaveEvent;
import it.water.core.api.model.events.Event;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.service.traffic.WaterTrafficEvent;
import it.water.core.service.traffic.WaterTrafficRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link TrafficDomainEventListener} (traffic pipeline phase F2).
 * <p>
 * <b>Scope.</b> This class stays a pure unit test even though an end-to-end harness now exists
 * ({@code DomainEventCaptureHarnessTest}, made possible by {@code InMemoryApplicationEventProducer}):
 * driving the callbacks directly is the only way to force every gate deterministically. It calls
 * the listener's public
 * {@code ApplicationEventListener} callbacks DIRECTLY with hand-built {@link Event} fixtures and
 * Mockito mocks for its single {@code @Inject(injectOnceAtStartup = true) ComponentRegistry}
 * dependency, which is both sufficient and deterministic for exercising every branch: the mandatory
 * loop guard, the ADR-11 reporter short-circuit, the ADR-2 opt-in/opt-out whitelist gate, the
 * CRUD-to-{@link RecordType}/{@link ChangePhase}/{@link ChangeOperation} mapping, the ADR-4
 * before/after reference formatting, and the fail-safe catch-all.
 */
@ExtendWith(MockitoExtension.class)
class TrafficDomainEventListenerTest {

    private static final String PROP_EVENTS_ENABLED = "water.traffic.events.enabled";
    private static final String PROP_EVENTS_MODE = "water.traffic.events.mode";
    private static final String PROP_EVENTS_WHITELIST = "water.traffic.events.whitelist";

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private TrafficReporter trafficReporter;

    @Mock
    private ApplicationProperties applicationProperties;

    private TrafficDomainEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new TrafficDomainEventListener();
        listener.setComponentsRegistry(componentRegistry);
    }

    /**
     * Wires ONLY the property gates: a resolvable {@link ApplicationProperties} whose
     * {@code water.traffic.events.enabled} is {@code true} and whose {@code water.traffic.events.mode}
     * is the given value. Deliberately does NOT stub the whitelist property: in {@code opt-out} mode
     * it is never read, and opt-in tests stub it explicitly with the exact value they need.
     * <p>
     * Kept separate from {@link #wireEnabledReporter(String)} because the property gates now run
     * BEFORE the reporter lookup: a test that stops at a gate must not stub the reporter at all, or
     * Mockito's strict stubs flag it as unnecessary stubbing.
     */
    private void wireProperties(String mode) {
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_ENABLED, "true")).thenReturn("true");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_MODE, "opt-in")).thenReturn(mode);
    }

    /**
     * The property gates of {@link #wireProperties(String)} plus an enabled {@link TrafficReporter}:
     * the full wiring needed by any test that expects a record to actually be reported.
     */
    private void wireEnabledReporter(String mode) {
        wireProperties(mode);
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
    }

    private TrafficDomainEventRecord captureRecord() {
        ArgumentCaptor<TrafficDomainEventRecord> captor = ArgumentCaptor.forClass(TrafficDomainEventRecord.class);
        verify(trafficReporter).reportDomainEvent(captor.capture(), any());
        return captor.getValue();
    }

    // ---------------------------------------------------------------------
    // Loop guard (mandatory, checked FIRST)
    // ---------------------------------------------------------------------

    @Test
    void consumerEvent_loopGuard_subjectIsTrafficRecord_doesNotInteractWithRegistry() {
        TrafficRecord trafficRecordSubject = WaterTrafficRecord.builder()
                .recordId("loop-guard-1")
                .recordType(RecordType.API)
                .build();

        listener.consumerEvent(trafficRecordSubject, new TestGenericEvent());

        verifyNoInteractions(componentRegistry);
    }

    @Test
    void consumerEvent_loopGuard_eventIsTrafficEvent_doesNotInteractWithRegistry() {
        TestDomainEntity subject = new TestDomainEntity(1L);
        Event trafficEvent = new WaterTrafficEvent<>(
                WaterTrafficRecord.builder().recordId("loop-guard-2").recordType(RecordType.API).build(),
                null, null);

        listener.consumerEvent(subject, trafficEvent);

        verifyNoInteractions(componentRegistry);
    }

    // ---------------------------------------------------------------------
    // ADR-2 property gates - evaluated BEFORE any registry lookup, because this listener sits on
    // the CRUD hot path and a registry lookup costs orders of magnitude more than a property read.
    // Every test below therefore also asserts that the reporter is NOT even resolved.
    // ---------------------------------------------------------------------

    @Test
    void consumerEvent_registryNull_doesNotThrow_noOp() {
        listener.setComponentsRegistry(null);

        assertDoesNotThrow(() -> listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent()));
    }

    @Test
    void capture_eventsGloballyDisabledViaProperty_noCapture_andReporterNeverResolved() {
        // deliberately NOT reusing wireProperties(mode): eventCaptureEnabled() short-circuits
        // before mode/whitelist are ever consulted, so stubbing them here would be flagged as
        // unnecessary stubbing under Mockito's strict stubs
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_ENABLED, "true")).thenReturn("false");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verifyNoInteractions(trafficReporter);
        verify(componentRegistry, never()).findComponents(TrafficReporter.class, null);
        verify(applicationProperties, never()).getPropertyOrDefault(PROP_EVENTS_MODE, "opt-in");
    }

    @Test
    void capture_optIn_emptyWhitelist_noCapture_andReporterNeverResolved() {
        wireProperties("opt-in");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_WHITELIST, "")).thenReturn("");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verifyNoInteractions(trafficReporter);
        verify(componentRegistry, never()).findComponents(TrafficReporter.class, null);
    }

    @Test
    void capture_applicationPropertiesUnresolvable_appliesDefaults_noCaptureBecauseWhitelistEmpty() {
        when(componentRegistry.findComponent(ApplicationProperties.class, null))
                .thenThrow(new NoComponentRegistryFoundException());

        // defaults: events enabled=true, mode=opt-in, whitelist=empty -> opt-in with empty whitelist
        // captures nothing, and the reporter lookup is never even attempted
        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verifyNoInteractions(trafficReporter);
        verify(componentRegistry, never()).findComponents(TrafficReporter.class, null);
    }

    @Test
    void capture_applicationPropertiesAlreadyResolved_isUsedWithoutAnyRegistryLookup() {
        listener.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_ENABLED, "true")).thenReturn("false");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verifyNoInteractions(componentRegistry);
        verifyNoInteractions(trafficReporter);
    }

    // ---------------------------------------------------------------------
    // ADR-11 reporter resolution short-circuit (reached only once the property gates passed)
    // ---------------------------------------------------------------------

    @Test
    void capture_findComponentsThrowsNoComponentRegistryFoundException_noOp() {
        wireProperties("opt-out");
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenThrow(new NoComponentRegistryFoundException());

        assertDoesNotThrow(() -> listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent()));

        verify(componentRegistry).findComponents(TrafficReporter.class, null);
    }

    @Test
    void capture_reporterListEmpty_noOp() {
        wireProperties("opt-out");
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of());

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verify(componentRegistry).findComponents(TrafficReporter.class, null);
        verifyNoInteractions(trafficReporter);
    }

    @Test
    void capture_reporterDisabled_noReportDomainEventCall() {
        wireProperties("opt-out");
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(false);

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        // isEnabled() itself IS a legitimate interaction - it is the short-circuit check
        verify(trafficReporter).isEnabled();
        verify(trafficReporter, never()).reportDomainEvent(any(), any());
        verifyNoMoreInteractions(trafficReporter);
        verify(componentRegistry).findComponents(TrafficReporter.class, null);
    }

    @Test
    void capture_optIn_whitelistContainsFqn_captures() {
        wireEnabledReporter("opt-in");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_WHITELIST, ""))
                .thenReturn(TestDomainEntity.class.getName());

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verify(trafficReporter).reportDomainEvent(any(), any());
    }

    @Test
    void capture_optIn_whitelistContainsSimpleName_captures() {
        wireEnabledReporter("opt-in");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_WHITELIST, "")).thenReturn("TestDomainEntity");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verify(trafficReporter).reportDomainEvent(any(), any());
    }

    @Test
    void capture_optIn_whitelistContainsOtherClass_noCapture_andReporterNeverResolved() {
        // a non-matching whitelist stops at the property gate, so the reporter must not be wired
        // here at all (strict stubs) - and must not even be looked up
        wireProperties("opt-in");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_WHITELIST, ""))
                .thenReturn("com.example.OtherResource");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verifyNoInteractions(trafficReporter);
        verify(componentRegistry, never()).findComponents(TrafficReporter.class, null);
    }

    @Test
    void capture_optIn_whitelistWithSpacesAndMultipleEntries_parsesAndCaptures() {
        wireEnabledReporter("opt-in");
        when(applicationProperties.getPropertyOrDefault(PROP_EVENTS_WHITELIST, ""))
                .thenReturn(" foo.Bar , TestDomainEntity , baz.Qux ");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verify(trafficReporter).reportDomainEvent(any(), any());
    }

    @Test
    void capture_optOut_capturesRegardlessOfWhitelist() {
        wireEnabledReporter("opt-out");

        listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent());

        verify(trafficReporter).reportDomainEvent(any(), any());
        // opt-out returns true BEFORE the whitelist is ever consulted
        verify(applicationProperties, never()).getPropertyOrDefault(PROP_EVENTS_WHITELIST, "");
    }

    @Test
    void capture_optOut_nullResourceType_stillCaptures() {
        wireEnabledReporter("opt-out");

        listener.consumerEvent(null, new TestGenericEvent());

        ArgumentCaptor<TrafficDomainEventRecord> recordCaptor = ArgumentCaptor.forClass(TrafficDomainEventRecord.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(trafficReporter).reportDomainEvent(recordCaptor.capture(), payloadCaptor.capture());
        assertNull(recordCaptor.getValue().resourceType());
        assertNull(recordCaptor.getValue().resourceId());
        assertNull(payloadCaptor.getValue());
    }

    // ---------------------------------------------------------------------
    // CRUD event -> RecordType/ChangePhase/ChangeOperation mapping
    // ---------------------------------------------------------------------

    @Test
    void capture_preSaveEvent_mapsToPreSavePersistenceRecord() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(10L);

        listener.consumerEvent(entity, new TestPreSaveEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.PRE, record.changePhase());
        assertEquals(ChangeOperation.SAVE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(TestDomainEntity.class.getName(), record.resourceType());
        assertEquals("10", record.resourceId());
        // the most specific Water event SUB-interface, not the concrete fixture class
        assertEquals(PreSaveEvent.class.getName(), record.eventClass());
        // plain (non-detailed) callback: no before/after images to disambiguate
        assertNull(record.beforeRef());
        assertNull(record.afterRef());
        assertNotNull(record.recordId());
        assertNotNull(record.timestamp());
        assertEquals(Outcome.SUCCESS, record.outcome());
        assertEquals(0L, record.durationMillis());
        assertEquals(1, record.schemaVersion());
    }

    @Test
    void capture_postSaveEvent_mapsToPostSavePersistenceRecord() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(11L);

        listener.consumerEvent(entity, new TestPostSaveEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.SAVE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(PostSaveEvent.class.getName(), record.eventClass());
    }

    @Test
    void capture_preUpdateEvent_mapsToUpdateOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(12L);

        listener.consumerEvent(entity, new TestPreUpdateEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.PRE, record.changePhase());
        assertEquals(ChangeOperation.UPDATE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
    }

    @Test
    void capture_postUpdateEvent_mapsToUpdateOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(13L);

        listener.consumerEvent(entity, new TestPostUpdateEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.UPDATE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
    }

    @Test
    void capture_preUpdateDetailedEvent_mapsBeforeAfterRefsAndUpdateOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity before = new TestDomainEntity(1L);
        TestDomainEntity after = new TestDomainEntity(2L);

        listener.consumerDetailedEvent(before, after, new TestPreUpdateDetailedEvent());

        ArgumentCaptor<TrafficDomainEventRecord> recordCaptor = ArgumentCaptor.forClass(TrafficDomainEventRecord.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(trafficReporter).reportDomainEvent(recordCaptor.capture(), payloadCaptor.capture());
        TrafficDomainEventRecord record = recordCaptor.getValue();

        assertEquals(ChangePhase.PRE, record.changePhase());
        assertEquals(ChangeOperation.UPDATE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(TestDomainEntity.class.getName() + "#1", record.beforeRef());
        assertEquals(TestDomainEntity.class.getName() + "#2", record.afterRef());
        // afterResource is non-null -> subject == afterResource
        assertEquals("2", record.resourceId());
        assertSame(after, payloadCaptor.getValue());
    }

    @Test
    void capture_postUpdateDetailedEvent_mapsBeforeAfterRefsAndUpdateOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity before = new TestDomainEntity(3L);
        TestDomainEntity after = new TestDomainEntity(4L);

        listener.consumerDetailedEvent(before, after, new TestPostUpdateDetailedEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.UPDATE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(TestDomainEntity.class.getName() + "#3", record.beforeRef());
        assertEquals(TestDomainEntity.class.getName() + "#4", record.afterRef());
    }

    @Test
    void capture_preRemoveEvent_mapsToRemoveOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(14L);

        listener.consumerEvent(entity, new TestPreRemoveEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.PRE, record.changePhase());
        assertEquals(ChangeOperation.REMOVE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
    }

    @Test
    void capture_postRemoveEvent_mapsToRemoveOperation() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(15L);

        listener.consumerEvent(entity, new TestPostRemoveEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangePhase.POST, record.changePhase());
        assertEquals(ChangeOperation.REMOVE, record.changeOperation());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
    }

    @Test
    void capture_genericNonCrudEvent_mapsToGenericDomainEvent() {
        wireEnabledReporter("opt-out");
        TestDomainEntity entity = new TestDomainEntity(20L);

        listener.consumerEvent(entity, new TestGenericEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertEquals(ChangeOperation.GENERIC, record.changeOperation());
        assertEquals(RecordType.DOMAIN_EVENT, record.recordType());
        assertNull(record.changePhase());
        assertEquals(TestGenericEvent.class.getName(), record.eventClass());
    }

    // ---------------------------------------------------------------------
    // Subject resolution / resourceId / ADR-4 references
    // ---------------------------------------------------------------------

    @Test
    void consumerDetailedEvent_nullAfterResource_subjectIsBeforeResource() {
        wireEnabledReporter("opt-out");
        TestDomainEntity before = new TestDomainEntity(5L);

        listener.consumerDetailedEvent(before, null, new TestPreUpdateDetailedEvent());

        ArgumentCaptor<TrafficDomainEventRecord> recordCaptor = ArgumentCaptor.forClass(TrafficDomainEventRecord.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(trafficReporter).reportDomainEvent(recordCaptor.capture(), payloadCaptor.capture());
        TrafficDomainEventRecord record = recordCaptor.getValue();

        assertEquals("5", record.resourceId());
        assertEquals(TestDomainEntity.class.getName() + "#5", record.beforeRef());
        assertNull(record.afterRef());
        assertSame(before, payloadCaptor.getValue());
    }

    @Test
    void capture_resourceNotBaseEntity_resourceIdIsNullAndRefHasNoId() {
        wireEnabledReporter("opt-out");
        TestPlainResource resource = new TestPlainResource();

        listener.consumerEvent(resource, new TestGenericEvent());

        TrafficDomainEventRecord record = captureRecord();
        assertNull(record.resourceId());
        assertEquals(TestPlainResource.class.getName(), record.resourceType());
        // the plain (non-detailed) callback never sets the before/after refs
        assertNull(record.beforeRef());
        assertNull(record.afterRef());
    }

    // ---------------------------------------------------------------------
    // Fail-safe
    // ---------------------------------------------------------------------

    @Test
    void capture_reporterThrows_isSwallowed_doesNotPropagate() {
        wireEnabledReporter("opt-out");
        doThrow(new RuntimeException("publish failed")).when(trafficReporter).reportDomainEvent(any(), any());

        assertDoesNotThrow(() -> listener.consumerEvent(new TestDomainEntity(1L), new TestPreSaveEvent()));
    }
}
