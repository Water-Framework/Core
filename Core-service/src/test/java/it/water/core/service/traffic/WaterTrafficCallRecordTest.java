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
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.api.traffic.model.TrafficRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficCallRecord}: verifies both the inherited
 * {@link WaterTrafficRecord} fields (via {@code @SuperBuilder} chaining) and the call-specific
 * (REST) fields, including the case where HTTP-only fields are left null for non-REST records.
 */
class WaterTrafficCallRecordTest {

    @Test
    void builder_setsInheritedAndCallSpecificFields() {
        Instant now = Instant.now();

        WaterTrafficCallRecord record = WaterTrafficCallRecord.builder()
                .recordId("call-1")
                .recordType(RecordType.REST)
                .timestamp(now)
                .serviceName("core-service")
                .moduleId("Core-service")
                .nodeId("node-1")
                .outcome(Outcome.SUCCESS)
                .durationMillis(50L)
                .source("client-a")
                .destination("service-b")
                .operation("GET /foo")
                .httpMethod("GET")
                .path("/foo")
                .statusCode(200)
                .clientIp("127.0.0.1")
                .argsRef("args-ref-1")
                .build();

        // inherited fields
        assertEquals("call-1", record.recordId());
        assertEquals(RecordType.REST, record.recordType());
        assertEquals(now, record.timestamp());
        assertEquals(1, record.schemaVersion());
        assertEquals(50L, record.durationMillis());

        // call-specific fields
        assertEquals("client-a", record.source());
        assertEquals("service-b", record.destination());
        assertEquals("GET /foo", record.operation());
        assertEquals("GET", record.httpMethod());
        assertEquals("/foo", record.path());
        assertEquals(Integer.valueOf(200), record.statusCode());
        assertEquals("127.0.0.1", record.clientIp());
        assertEquals("args-ref-1", record.argsRef());

        assertNotNull(record.getResourceName());
        assertTrue(record instanceof TrafficRecord);
        assertTrue(record instanceof TrafficCallRecord);
    }

    @Test
    void builder_nonRestFields_canBeLeftNull() {
        WaterTrafficCallRecord record = WaterTrafficCallRecord.builder()
                .recordId("call-2")
                .recordType(RecordType.API)
                .operation("MyApi.save")
                .build();

        assertEquals("MyApi.save", record.operation());
        assertNull(record.httpMethod());
        assertNull(record.path());
        assertNull(record.statusCode());
        assertNull(record.clientIp());
        assertNull(record.source());
        assertNull(record.destination());
        assertNull(record.argsRef());
    }
}
