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

import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficRecord}: no Water framework harness is required since
 * this class is an immutable, {@code @SuperBuilder}-generated value object with no
 * {@code @FrameworkComponent}/{@code @Inject} wiring.
 */
class WaterTrafficRecordTest {

    @Test
    void builder_setsAllFields_andAccessorsReturnExpectedValues() {
        Instant now = Instant.now();
        Map<String, String> metadata = Map.of("key1", "value1");

        WaterTrafficRecord record = WaterTrafficRecord.builder()
                .recordId("rec-1")
                .recordType(RecordType.API)
                .timestamp(now)
                .serviceName("core-service")
                .moduleId("Core-service")
                .nodeId("node-1")
                .correlationId("corr-1")
                .traceId("trace-1")
                .identity("user-1")
                .tenantId(42L)
                .outcome(Outcome.SUCCESS)
                .durationMillis(123L)
                .metadata(metadata)
                .build();

        assertEquals("rec-1", record.recordId());
        assertEquals(RecordType.API, record.recordType());
        assertEquals(now, record.timestamp());
        assertEquals("core-service", record.serviceName());
        assertEquals("Core-service", record.moduleId());
        assertEquals("node-1", record.nodeId());
        assertEquals("corr-1", record.correlationId());
        assertEquals("trace-1", record.traceId());
        assertEquals("user-1", record.identity());
        assertEquals(Long.valueOf(42L), record.tenantId());
        assertEquals(Outcome.SUCCESS, record.outcome());
        assertNull(record.errorType());
        assertNull(record.errorMessage());
        assertEquals(123L, record.durationMillis());
        assertEquals(metadata, record.metadata());
        // @Builder.Default: schemaVersion defaults to 1 when not explicitly set
        assertEquals(1, record.schemaVersion());
        // default method inherited from Resource
        assertNotNull(record.getResourceName());
        assertTrue(record instanceof TrafficRecord);
    }

    @Test
    void builder_schemaVersionExplicitOverride_isRespected() {
        WaterTrafficRecord record = WaterTrafficRecord.builder()
                .recordId("rec-2")
                .recordType(RecordType.PERSISTENCE)
                .schemaVersion(7)
                .build();

        assertEquals(7, record.schemaVersion());
    }

    @Test
    void builder_errorOutcome_carriesErrorFields() {
        WaterTrafficRecord record = WaterTrafficRecord.builder()
                .recordId("rec-3")
                .recordType(RecordType.SYSTEM_API)
                .outcome(Outcome.ERROR)
                .errorType("java.lang.RuntimeException")
                .errorMessage("boom")
                .build();

        assertEquals(Outcome.ERROR, record.outcome());
        assertEquals("java.lang.RuntimeException", record.errorType());
        assertEquals("boom", record.errorMessage());
    }

    @Test
    void builder_optionalFieldsLeftUnset_areNull() {
        WaterTrafficRecord record = WaterTrafficRecord.builder()
                .recordId("rec-4")
                .recordType(RecordType.REST)
                .build();

        assertNull(record.serviceName());
        assertNull(record.moduleId());
        assertNull(record.nodeId());
        assertNull(record.correlationId());
        assertNull(record.traceId());
        assertNull(record.identity());
        assertNull(record.tenantId());
        assertNull(record.outcome());
        assertNull(record.metadata());
    }
}
