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
package it.water.core.service.traffic.rest;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.service.traffic.TrafficCorrelationContext;
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
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link RestTrafficCaptureImpl} (traffic pipeline phase F3, incoming REST
 * capture; F4 adds the correlation scope). Mirrors the Mockito-on-{@code ComponentRegistry}/
 * {@code ApplicationProperties}/{@code TrafficReporter} pattern already established in
 * {@code it.water.core.service.traffic.events.TrafficDomainEventListenerTest}: the class under test
 * has a single {@code @Inject(injectOnceAtStartup = true) ComponentRegistry} dependency and no
 * lifecycle callbacks, so mocking the registry directly gives full, deterministic control over
 * every short-circuit (registry-null, findComponents/findComponent throwing, reporter disabled,
 * property-driven enablement, exclusions) without any real DI wiring.
 * <p>
 * <b>F4 note.</b> Identity/tenant enrichment and error-message truncation are NO LONGER this
 * class's responsibility - they moved to {@code TrafficRecordPolicy}, applied centrally behind the
 * {@code TrafficReporter} for every capture source alike. Their coverage lives in
 * {@code it.water.core.service.traffic.policy.TrafficRecordPolicyTest}; this class only needs to
 * prove it hands the FULL, untouched error message to {@code reportCall} and correctly opens/closes
 * the {@link TrafficCorrelationContext} scope around the request.
 * <p>
 * The CXF and Spring per-runtime adapters that actually call into this class are separately unit
 * tested (with their own runtime's mocks/test doubles) in
 * {@code it.water.service.rest.manager.cxf.traffic.CxfTrafficCaptureFilterTest} and
 * {@code it.water.service.rest.spring.traffic.SpringTrafficCaptureInterceptorTest}.
 */
@ExtendWith(MockitoExtension.class)
class RestTrafficCaptureImplTest {

    private static final String PROP_REST_ENABLED = "water.traffic.rest.enabled";
    private static final String PROP_REST_EXCLUDE = "water.traffic.rest.exclude";

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private TrafficReporter trafficReporter;

    @Mock
    private ApplicationProperties applicationProperties;

    private RestTrafficCaptureImpl impl;

    @BeforeEach
    void setUp() {
        impl = new RestTrafficCaptureImpl();
        impl.setComponentsRegistry(componentRegistry);
        // belt-and-braces: a scope leaked by a previous test (in this class or another, on the same
        // thread) must never leak INTO a test either
        TrafficCorrelationContext.close();
    }

    private void wireEnabledReporterAndRestEnabled() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_REST_ENABLED, "true")).thenReturn("true");
    }

    /**
     * For scenarios where the normalized path ends up {@code null} (null/blank input): the
     * exclusions property is never consulted in that case (see {@code isExcluded}'s own
     * {@code normalizedPath == null} short-circuit), so stubbing it here would be flagged as
     * unnecessary stubbing under Mockito's strict stubs.
     */
    private void wireEnabledCaptureNoExclusions() {
        wireEnabledReporterAndRestEnabled();
    }

    private void wireEnabledCaptureWithExclude(String excludeValue) {
        wireEnabledReporterAndRestEnabled();
        when(applicationProperties.getPropertyOrDefault(PROP_REST_EXCLUDE, "")).thenReturn(excludeValue);
    }

    private TrafficCallRecord captureRecord(String httpMethod, String path, Integer statusCode, String clientIp,
                                             long durationMillis, Throwable error) {
        impl.captureRestCall(httpMethod, path, statusCode, clientIp, durationMillis, error);
        ArgumentCaptor<TrafficCallRecord> captor = ArgumentCaptor.forClass(TrafficCallRecord.class);
        verify(trafficReporter).reportCall(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------------
    // isEnabled()
    // ---------------------------------------------------------------------

    @Test
    void isEnabled_registryNull_returnsFalse() {
        impl.setComponentsRegistry(null);

        assertFalse(impl.isEnabled());
    }

    @Test
    void isEnabled_findComponentsThrows_returnsFalse() {
        when(componentRegistry.findComponents(TrafficReporter.class, null))
                .thenThrow(new NoComponentRegistryFoundException());

        assertFalse(impl.isEnabled());
    }

    @Test
    void isEnabled_reporterListEmpty_returnsFalse() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of());

        assertFalse(impl.isEnabled());
    }

    @Test
    void isEnabled_reporterDisabled_returnsFalse() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(false);

        assertFalse(impl.isEnabled());
        // the short-circuit itself asks isEnabled(): that is the ONLY allowed interaction
        verify(trafficReporter).isEnabled();
    }

    @Test
    void isEnabled_reporterEnabledAndRestEnabledTrue_returnsTrue() {
        wireEnabledReporterAndRestEnabled();

        assertTrue(impl.isEnabled());
    }

    @Test
    void isEnabled_reporterEnabledButRestEnabledFalse_returnsFalse() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_REST_ENABLED, "true")).thenReturn("false");

        assertFalse(impl.isEnabled());
    }

    @Test
    void isEnabled_applicationPropertiesUnresolvable_defaultsToTrue() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
        when(componentRegistry.findComponent(ApplicationProperties.class, null))
                .thenThrow(new NoComponentRegistryFoundException());

        assertTrue(impl.isEnabled());
    }

    // ---------------------------------------------------------------------
    // requestStarted() / correlation scope (F4)
    // ---------------------------------------------------------------------

    @Test
    void requestStarted_opensCorrelationScope() {
        try {
            impl.requestStarted();

            assertNotNull(TrafficCorrelationContext.current());
        } finally {
            TrafficCorrelationContext.close();
        }
    }

    @Test
    void captureRestCall_successfulCapture_stillClosesCorrelationScope() {
        try {
            TrafficCorrelationContext.open();
            wireEnabledCaptureWithExclude("");

            impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

            assertNull(TrafficCorrelationContext.current());
        } finally {
            TrafficCorrelationContext.close();
        }
    }

    @Test
    void captureRestCall_reporterAbsent_stillClosesCorrelationScope() {
        try {
            TrafficCorrelationContext.open();
            when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of());

            impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

            assertNull(TrafficCorrelationContext.current());
        } finally {
            TrafficCorrelationContext.close();
        }
    }

    @Test
    void captureRestCall_pathExcluded_stillClosesCorrelationScope() {
        try {
            TrafficCorrelationContext.open();
            wireEnabledCaptureWithExclude("/roles");

            impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

            assertNull(TrafficCorrelationContext.current());
        } finally {
            TrafficCorrelationContext.close();
        }
    }

    @Test
    void captureRestCall_reportCallThrows_stillClosesCorrelationScope() {
        try {
            TrafficCorrelationContext.open();
            wireEnabledCaptureWithExclude("");
            doThrow(new RuntimeException("boom")).when(trafficReporter).reportCall(any());

            impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

            assertNull(TrafficCorrelationContext.current());
        } finally {
            TrafficCorrelationContext.close();
        }
    }

    // ---------------------------------------------------------------------
    // captureRestCall() short-circuits
    // ---------------------------------------------------------------------

    @Test
    void captureRestCall_reporterListEmpty_noReportCallNoException() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of());

        assertDoesNotThrow(() -> impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null));
    }

    @Test
    void captureRestCall_reporterDisabled_noReportCall() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(false);

        impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

        verify(trafficReporter).isEnabled();
        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void captureRestCall_restDisabledViaProperty_noReportCall() {
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(PROP_REST_ENABLED, "true")).thenReturn("false");

        impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

        verify(trafficReporter, never()).reportCall(any());
    }

    // ---------------------------------------------------------------------
    // Exclusions (prefix match on the normalized path)
    // ---------------------------------------------------------------------

    @Test
    void captureRestCall_pathExcludedByPrefix_noReportCall() {
        wireEnabledCaptureWithExclude("/health");

        impl.captureRestCall("GET", "/health/db", 200, "127.0.0.1", 5L, null);

        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void captureRestCall_excludeListWithSpacesAndMultipleEntries_matchesOne_excluded() {
        wireEnabledCaptureWithExclude(" /metrics , /health , /actuator ");

        impl.captureRestCall("GET", "/health/live", 200, "127.0.0.1", 5L, null);

        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void captureRestCall_excludeEntryDoesNotMatch_captured() {
        wireEnabledCaptureWithExclude("/metrics");

        impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 5L, null);

        verify(trafficReporter).reportCall(any());
    }

    // ---------------------------------------------------------------------
    // Path normalization (verified on the captured record - the convergence point between
    // the CXF and the Spring adapters)
    // ---------------------------------------------------------------------

    @Test
    void normalizePath_waterPrefixed_stripsBaseContext() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/water/roles", 200, "127.0.0.1", 5L, null);

        assertEquals("/roles", record.path());
    }

    @Test
    void normalizePath_alreadyNormalized_unchanged() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/roles", 200, "127.0.0.1", 5L, null);

        assertEquals("/roles", record.path());
    }

    @Test
    void normalizePath_missingLeadingSlash_getsOne() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "water/roles", 200, "127.0.0.1", 5L, null);

        assertEquals("/roles", record.path());
    }

    @Test
    void normalizePath_bareWaterContext_becomesRootSlash() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/water", 200, "127.0.0.1", 5L, null);

        assertEquals("/", record.path());
    }

    @Test
    void normalizePath_trailingSlash_collapsed() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/water/roles/", 200, "127.0.0.1", 5L, null);

        assertEquals("/roles", record.path());
    }

    @Test
    void normalizePath_queryString_stripped() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/roles?filter=x", 200, "127.0.0.1", 5L, null);

        assertEquals("/roles", record.path());
    }

    @Test
    void normalizePath_nullPath_recordPathIsNull() {
        wireEnabledCaptureNoExclusions();

        TrafficCallRecord record = captureRecord("GET", null, 200, "127.0.0.1", 5L, null);

        assertNull(record.path());
    }

    @Test
    void normalizePath_blankPath_recordPathIsNull() {
        wireEnabledCaptureNoExclusions();

        TrafficCallRecord record = captureRecord("GET", "   ", 200, "127.0.0.1", 5L, null);

        assertNull(record.path());
    }

    // ---------------------------------------------------------------------
    // Record mapping / outcome derivation
    // ---------------------------------------------------------------------

    @Test
    void captureRestCall_mapsCommonAndRestSpecificFields() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/roles", 200, "10.0.0.1", 42L, null);

        assertEquals(RecordType.REST, record.recordType());
        assertEquals("GET /roles", record.operation());
        assertEquals("GET", record.httpMethod());
        assertEquals("/roles", record.path());
        assertEquals(Integer.valueOf(200), record.statusCode());
        assertEquals("10.0.0.1", record.clientIp());
        assertEquals(42L, record.durationMillis());
        assertEquals(1, record.schemaVersion());
        assertNotNull(record.recordId());
        assertNotNull(record.timestamp());
        assertEquals(Outcome.SUCCESS, record.outcome());
    }

    @Test
    void captureRestCall_statusCode404_outcomeError() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/roles/1", 404, "127.0.0.1", 3L, null);

        assertEquals(Outcome.ERROR, record.outcome());
    }

    @Test
    void captureRestCall_statusCode500_outcomeError() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("POST", "/roles", 500, "127.0.0.1", 3L, null);

        assertEquals(Outcome.ERROR, record.outcome());
    }

    @Test
    void captureRestCall_statusCodeNullAndNoError_outcomeSuccess() {
        wireEnabledCaptureWithExclude("");

        TrafficCallRecord record = captureRecord("GET", "/roles", null, "127.0.0.1", 3L, null);

        assertEquals(Outcome.SUCCESS, record.outcome());
        assertNull(record.statusCode());
    }

    @Test
    void captureRestCall_errorNonNull_outcomeErrorWithTypeAndMessage() {
        wireEnabledCaptureWithExclude("");
        IllegalStateException error = new IllegalStateException("boom");

        TrafficCallRecord record = captureRecord("GET", "/roles", null, "127.0.0.1", 3L, error);

        assertEquals(Outcome.ERROR, record.outcome());
        assertEquals(IllegalStateException.class.getName(), record.errorType());
        assertEquals("boom", record.errorMessage());
    }

    /**
     * F4: truncation is now a {@code TrafficRecordPolicy} concern (ADR-4), applied centrally behind
     * the reporter. This class must hand the FULL, untouched message to {@code reportCall} - the
     * truncated-to-200 assertion moved to {@code TrafficRecordPolicyTest}.
     */
    @Test
    void captureRestCall_longErrorMessage_reachesReporterIntact() {
        wireEnabledCaptureWithExclude("");
        String longMessage = "x".repeat(250);
        RuntimeException error = new RuntimeException(longMessage);

        TrafficCallRecord record = captureRecord("GET", "/roles", 500, "127.0.0.1", 3L, error);

        assertEquals(longMessage, record.errorMessage());
        assertEquals(250, record.errorMessage().length());
    }

    @Test
    void captureRestCall_errorWithNullMessage_errorMessageNull() {
        wireEnabledCaptureWithExclude("");
        RuntimeException error = new RuntimeException();

        TrafficCallRecord record = captureRecord("GET", "/roles", 500, "127.0.0.1", 3L, error);

        assertEquals(RuntimeException.class.getName(), record.errorType());
        assertNull(record.errorMessage());
    }

    // ---------------------------------------------------------------------
    // Fail-safe
    // ---------------------------------------------------------------------

    @Test
    void captureRestCall_reporterReportCallThrows_doesNotPropagate() {
        wireEnabledCaptureWithExclude("");
        doThrow(new RuntimeException("publish failed")).when(trafficReporter).reportCall(any());

        assertDoesNotThrow(() -> impl.captureRestCall("GET", "/roles", 200, "127.0.0.1", 3L, null));
    }
}
