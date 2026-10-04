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
package it.water.core.service.traffic.policy;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.service.traffic.TrafficCorrelationContext;
import it.water.core.service.traffic.WaterTrafficCallRecord;
import it.water.core.service.traffic.WaterTrafficDomainEventRecord;
import it.water.core.service.traffic.WaterTrafficRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TrafficRecordPolicy} (F4): enrichment, redaction (ADR-4) and sampling
 * (ADR-5), the three cross-cutting concerns applied to every traffic record behind the reporter.
 * <p>
 * Mockito on {@link ApplicationProperties}/{@link Runtime}/{@link SecurityContext}/
 * {@link ClusterNodeOptions} (the same style already established for
 * {@code it.water.core.service.traffic.events.TrafficDomainEventListenerTest}): the class under
 * test is a plain, stateless, constructor-injected collaborator with no framework lifecycle, so
 * direct mocking gives full, deterministic control over every branch.
 * <p>
 * <b>Sampling and hash buckets.</b> {@code shouldReport}'s intermediate-rate branch hashes a key
 * into one of {@link TrafficRecordPolicy#SAMPLING_BUCKETS} buckets via
 * {@code (key.hashCode() & Integer.MAX_VALUE) % SAMPLING_BUCKETS}. Tests that need a key
 * deterministically INSIDE or OUTSIDE a given rate threshold compute the bucket with that exact
 * formula themselves (see {@link #bucketOf(String)}/{@link #findKeyWithBucket(Predicate, String)})
 * rather than hardcoding any hash value - {@code String#hashCode()} is specified/stable across JVM
 * runs, so the search is deterministic and fast.
 * <p>
 * <b>Correlation hygiene.</b> Every test that opens a {@link TrafficCorrelationContext} scope is
 * cleaned up by {@link #clearCorrelationScope()} below, so nothing leaks into another test class on
 * the same thread.
 */
@ExtendWith(MockitoExtension.class)
class TrafficRecordPolicyTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private Runtime runtime;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ClusterNodeOptions clusterNodeOptions;

    @AfterEach
    void clearCorrelationScope() {
        TrafficCorrelationContext.close();
    }

    private int bucketOf(String key) {
        return (key.hashCode() & Integer.MAX_VALUE) % TrafficRecordPolicy.SAMPLING_BUCKETS;
    }

    private String findKeyWithBucket(Predicate<String> bucketMatches, String prefix) {
        for (int i = 0; i < 100_000; i++) {
            String candidate = prefix + i;
            if (bucketMatches.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("no candidate key found matching the bucket predicate in range - "
                + "broaden the search, String#hashCode() distribution should not require this");
    }

    // ---------------------------------------------------------------------
    // normalize() - enrichment
    // ---------------------------------------------------------------------

    @Test
    void normalize_fillsIdentityAndTenantIdFromSecurityContext() {
        when(runtime.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getLoggedUsername()).thenReturn("alice");
        when(securityContext.getActiveCompanyId()).thenReturn(42L);
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, runtime, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r1").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals("alice", normalized.identity());
        assertEquals(Long.valueOf(42L), normalized.tenantId());
    }

    @Test
    void normalize_fillsCorrelationIdFromOpenScope() {
        String correlationId = TrafficCorrelationContext.open();
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r2").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals(correlationId, normalized.correlationId());
    }

    @Test
    void normalize_noOpenScope_correlationIdStaysNull() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r3").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertNull(normalized.correlationId());
    }

    @Test
    void normalize_fillsNodeIdFromClusterNodeOptions() {
        when(clusterNodeOptions.getNodeId()).thenReturn("node-7");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, clusterNodeOptions);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r4").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals("node-7", normalized.nodeId());
    }

    @Test
    void normalize_fillsServiceNameFromProperty() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SERVICE_NAME, ""))
                .thenReturn("core-service");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r5").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals("core-service", normalized.serviceName());
    }

    @Test
    void normalize_serviceNameBlank_staysNull() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SERVICE_NAME, ""))
                .thenReturn("   ");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r6").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertNull(normalized.serviceName());
    }

    @Test
    void normalize_neverOverwritesAlreadySetFields() {
        when(runtime.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getLoggedUsername()).thenReturn("someone-else");
        when(securityContext.getActiveCompanyId()).thenReturn(999L);
        when(clusterNodeOptions.getNodeId()).thenReturn("other-node");
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SERVICE_NAME, ""))
                .thenReturn("other-service");
        String openScopeCorrelationId = TrafficCorrelationContext.open();
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, runtime, clusterNodeOptions);
        TrafficRecord record = WaterTrafficRecord.builder()
                .recordId("r7")
                .recordType(RecordType.API)
                .identity("original-identity")
                .tenantId(1L)
                .correlationId("original-correlation")
                .nodeId("original-node")
                .serviceName("original-service")
                .build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals("original-identity", normalized.identity());
        assertEquals(Long.valueOf(1L), normalized.tenantId());
        assertEquals("original-correlation", normalized.correlationId());
        assertEquals("original-node", normalized.nodeId());
        assertEquals("original-service", normalized.serviceName());
        assertNotEquals(openScopeCorrelationId, normalized.correlationId());
    }

    @Test
    void normalize_runtimeNull_identityAndTenantStayNullNoException() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, clusterNodeOptions);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r-runtime-null").recordType(RecordType.API).build();

        TrafficRecord normalized = assertDoesNotThrow(() -> policy.normalize(record));

        assertNull(normalized.identity());
        assertNull(normalized.tenantId());
    }

    @Test
    void normalize_securityContextNull_identityAndTenantStayNullNoException() {
        when(runtime.getSecurityContext()).thenReturn(null);
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, runtime, clusterNodeOptions);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r-secctx-null").recordType(RecordType.API).build();

        TrafficRecord normalized = assertDoesNotThrow(() -> policy.normalize(record));

        assertNull(normalized.identity());
        assertNull(normalized.tenantId());
    }

    @Test
    void normalize_clusterNodeOptionsNull_nodeIdStaysNullNoException() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, runtime, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r-cno-null").recordType(RecordType.API).build();

        TrafficRecord normalized = assertDoesNotThrow(() -> policy.normalize(record));

        assertNull(normalized.nodeId());
    }

    @Test
    void normalize_applicationPropertiesNull_serviceNameStaysNullNoException() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(null, runtime, clusterNodeOptions);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("r-props-null").recordType(RecordType.API).build();

        TrafficRecord normalized = assertDoesNotThrow(() -> policy.normalize(record));

        assertNull(normalized.serviceName());
    }

    @Test
    void normalize_preservesConcreteType_callRecordFieldsIntact() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        WaterTrafficCallRecord record = WaterTrafficCallRecord.builder()
                .recordId("call-1")
                .recordType(RecordType.REST)
                .httpMethod("GET")
                .path("/roles")
                .statusCode(200)
                .clientIp("127.0.0.1")
                .build();

        WaterTrafficCallRecord normalized = policy.normalize(record);

        assertEquals("GET", normalized.httpMethod());
        assertEquals("/roles", normalized.path());
        assertEquals(Integer.valueOf(200), normalized.statusCode());
        assertEquals("127.0.0.1", normalized.clientIp());
    }

    @Test
    void normalize_preservesConcreteType_domainEventRecordFieldsIntact() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        WaterTrafficDomainEventRecord record = WaterTrafficDomainEventRecord.builder()
                .recordId("de-1")
                .recordType(RecordType.PERSISTENCE)
                .eventClass("it.water.core.api.entity.events.PostSaveEvent")
                .changePhase(ChangePhase.POST)
                .changeOperation(ChangeOperation.SAVE)
                .resourceId("42")
                .build();

        WaterTrafficDomainEventRecord normalized = policy.normalize(record);

        assertEquals("it.water.core.api.entity.events.PostSaveEvent", normalized.eventClass());
        assertEquals(ChangePhase.POST, normalized.changePhase());
        assertEquals(ChangeOperation.SAVE, normalized.changeOperation());
        assertEquals("42", normalized.resourceId());
    }

    @Test
    void normalize_nonWaterTrafficRecordImplementation_returnedAsIs() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, runtime, clusterNodeOptions);
        TrafficRecord record = new NonWaterTrafficRecord();

        TrafficRecord normalized = policy.normalize(record);

        assertSame(record, normalized);
    }

    // ---------------------------------------------------------------------
    // normalize() - redaction (ADR-4)
    // ---------------------------------------------------------------------

    @Test
    void normalize_errorMessageLongerThan200_truncatedTo200() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        String longMessage = "x".repeat(250);
        TrafficRecord record = WaterTrafficRecord.builder()
                .recordId("err-1")
                .recordType(RecordType.API)
                .errorMessage(longMessage)
                .build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals(200, normalized.errorMessage().length());
        assertEquals(longMessage.substring(0, 200), normalized.errorMessage());
    }

    @Test
    void normalize_errorMessageExactly200_intact() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        String message = "x".repeat(200);
        TrafficRecord record = WaterTrafficRecord.builder()
                .recordId("err-2")
                .recordType(RecordType.API)
                .errorMessage(message)
                .build();

        TrafficRecord normalized = policy.normalize(record);

        assertEquals(message, normalized.errorMessage());
    }

    @Test
    void normalize_errorMessageNull_staysNull() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("err-3").recordType(RecordType.API).build();

        TrafficRecord normalized = policy.normalize(record);

        assertNull(normalized.errorMessage());
    }

    @Test
    void normalize_payloadCaptureDefaultFalse_argsRefRedactedOnCallRecord() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        WaterTrafficCallRecord record = WaterTrafficCallRecord.builder()
                .recordId("call-2")
                .recordType(RecordType.REST)
                .argsRef("some-args-ref")
                .build();

        WaterTrafficCallRecord normalized = policy.normalize(record);

        assertNull(normalized.argsRef());
    }

    @Test
    void normalize_payloadCaptureExplicitlyTrue_argsRefPreserved() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_PAYLOAD_CAPTURE, "false"))
                .thenReturn("true");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        WaterTrafficCallRecord record = WaterTrafficCallRecord.builder()
                .recordId("call-3")
                .recordType(RecordType.REST)
                .argsRef("some-args-ref")
                .build();

        WaterTrafficCallRecord normalized = policy.normalize(record);

        assertEquals("some-args-ref", normalized.argsRef());
    }

    @Test
    void normalize_nonCallRecord_payloadRedactionDoesNotError() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("plain-1").recordType(RecordType.API).build();

        TrafficRecord normalized = assertDoesNotThrow(() -> policy.normalize(record));

        assertNotNull(normalized);
    }

    // ---------------------------------------------------------------------
    // shouldReport() - sampling (ADR-5)
    // ---------------------------------------------------------------------

    @Test
    void shouldReport_defaultRateOne_alwaysTrue() {
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("s1").recordType(RecordType.API).build();

        assertTrue(policy.shouldReport(record));
    }

    @Test
    void shouldReport_rateZero_alwaysFalse() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("0.0");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("s2").recordType(RecordType.API).build();

        assertFalse(policy.shouldReport(record));
    }

    @Test
    void shouldReport_nonNumericRate_fallsBackToDefaultTrue() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("not-a-number");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("s3").recordType(RecordType.API).build();

        assertTrue(policy.shouldReport(record));
    }

    /**
     * ADR-7: a whole request tree shares one sampling verdict. Proven here by checking REPEATED
     * calls (both on the same record, and across two DIFFERENT records that merely share the same
     * {@code correlationId}) always agree, at an intermediate rate where the decision is genuinely
     * data-dependent rather than trivially true/false.
     */
    @Test
    void shouldReport_sameCorrelationId_deterministicVerdictAcrossRecordsAndCalls() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("0.5");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord recordA = WaterTrafficRecord.builder()
                .recordId("rA").recordType(RecordType.API).correlationId("corr-fixed").build();
        TrafficRecord recordB = WaterTrafficRecord.builder()
                .recordId("rB").recordType(RecordType.API).correlationId("corr-fixed").build();

        boolean firstVerdict = policy.shouldReport(recordA);
        for (int i = 0; i < 20; i++) {
            assertEquals(firstVerdict, policy.shouldReport(recordA), "must be stable across repeated calls");
            assertEquals(firstVerdict, policy.shouldReport(recordB),
                    "must agree across different records sharing the same correlationId");
        }
    }

    @Test
    void shouldReport_noCorrelationId_fallsBackToRecordIdAndVariesByKey() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("0.5");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        int threshold = (int) (0.5d * TrafficRecordPolicy.SAMPLING_BUCKETS);
        String insideKey = findKeyWithBucket(candidate -> bucketOf(candidate) < threshold, "in-");
        String outsideKey = findKeyWithBucket(candidate -> bucketOf(candidate) >= threshold, "out-");
        TrafficRecord insideRecord = WaterTrafficRecord.builder().recordId(insideKey).recordType(RecordType.API).build();
        TrafficRecord outsideRecord = WaterTrafficRecord.builder().recordId(outsideKey).recordType(RecordType.API).build();

        assertTrue(policy.shouldReport(insideRecord), "the computed 'inside' key must be sampled in");
        assertFalse(policy.shouldReport(outsideRecord), "the computed 'outside' key must be sampled out");
        // stability check for the very same record without a correlationId
        assertTrue(policy.shouldReport(insideRecord));
    }

    @Test
    void shouldReport_perTypeScopeDefault_overridesRestToZero_apiStillUsesGlobalRate() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("1.0");
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_SCOPE, TrafficRecordPolicy.SCOPE_PER_TYPE))
                .thenReturn(TrafficRecordPolicy.SCOPE_PER_TYPE);
        String restOverrideKey = TrafficRecordPolicy.PROP_SAMPLING_RATE + "." + RecordType.REST.name().toLowerCase(Locale.ROOT);
        when(applicationProperties.getPropertyOrDefault(restOverrideKey, null)).thenReturn("0.0");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord restRecord = WaterTrafficRecord.builder().recordId("rest-1").recordType(RecordType.REST).build();
        TrafficRecord apiRecord = WaterTrafficRecord.builder().recordId("api-1").recordType(RecordType.API).build();

        assertFalse(policy.shouldReport(restRecord));
        assertTrue(policy.shouldReport(apiRecord));
    }

    @Test
    void shouldReport_globalScope_ignoresPerTypeOverride() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("1.0");
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_SCOPE, TrafficRecordPolicy.SCOPE_PER_TYPE))
                .thenReturn("global");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord restRecord = WaterTrafficRecord.builder().recordId("rest-3").recordType(RecordType.REST).build();

        assertTrue(policy.shouldReport(restRecord));

        // the per-type REST override, if configured, must never even be consulted under "global" scope
        verify(applicationProperties, never()).getPropertyOrDefault(
                eq(TrafficRecordPolicy.PROP_SAMPLING_RATE + "." + RecordType.REST.name().toLowerCase(Locale.ROOT)), any());
    }

    @Test
    void shouldReport_recordTypeNull_appliesGlobalRate() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("0.0");
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_SCOPE, TrafficRecordPolicy.SCOPE_PER_TYPE))
                .thenReturn(TrafficRecordPolicy.SCOPE_PER_TYPE);
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);
        TrafficRecord record = WaterTrafficRecord.builder().recordId("null-type-1").recordType(null).build();

        assertFalse(policy.shouldReport(record));
    }

    @Test
    void shouldReport_nullRecord_doesNotThrow() {
        when(applicationProperties.getPropertyOrDefault(TrafficRecordPolicy.PROP_SAMPLING_RATE, null))
                .thenReturn("0.5");
        TrafficRecordPolicy policy = new TrafficRecordPolicy(applicationProperties, null, null);

        assertDoesNotThrow(() -> policy.shouldReport(null));
    }

    /**
     * A {@link TrafficRecord} implementation that is deliberately NOT a {@code WaterTrafficRecord}:
     * exercises {@code normalize}'s pass-through branch for third-party record implementations,
     * which cannot be rebuilt via a Lombok {@code toBuilder()} they do not have.
     */
    private static final class NonWaterTrafficRecord implements TrafficRecord {
        @Override
        public String recordId() {
            return "ext-1";
        }

        @Override
        public RecordType recordType() {
            return RecordType.API;
        }

        @Override
        public Instant timestamp() {
            return null;
        }

        @Override
        public String serviceName() {
            return null;
        }

        @Override
        public String moduleId() {
            return null;
        }

        @Override
        public String nodeId() {
            return null;
        }

        @Override
        public String correlationId() {
            return null;
        }

        @Override
        public String traceId() {
            return null;
        }

        @Override
        public String identity() {
            return null;
        }

        @Override
        public Long tenantId() {
            return null;
        }

        @Override
        public Outcome outcome() {
            return Outcome.SUCCESS;
        }

        @Override
        public String errorType() {
            return null;
        }

        @Override
        public String errorMessage() {
            return null;
        }

        @Override
        public long durationMillis() {
            return 0L;
        }

        @Override
        public Map<String, String> metadata() {
            return null;
        }

        @Override
        public int schemaVersion() {
            return 1;
        }
    }
}
