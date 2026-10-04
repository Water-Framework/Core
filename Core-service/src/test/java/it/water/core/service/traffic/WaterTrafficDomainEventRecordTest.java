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

import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.api.traffic.model.TrafficRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficDomainEventRecord}: verifies both the inherited
 * {@link WaterTrafficRecord} fields (via {@code @SuperBuilder} chaining) and the domain-event
 * specific fields, including the case where {@code beforeRef}/{@code afterRef} are left null for
 * non-detailed events, which carry a single resource image rather than a before/after pair.
 */
class WaterTrafficDomainEventRecordTest {

    @Test
    void builder_setsInheritedAndDomainEventSpecificFields() {
        Instant now = Instant.now();

        WaterTrafficDomainEventRecord record = WaterTrafficDomainEventRecord.builder()
                .recordId("de-1")
                .recordType(RecordType.PERSISTENCE)
                .timestamp(now)
                .serviceName("core-service")
                .moduleId("Core-service")
                .nodeId("node-1")
                .outcome(Outcome.SUCCESS)
                .durationMillis(0L)
                .eventClass("it.water.core.api.entity.events.PreSaveEvent")
                .resourceType("it.water.SomeEntity")
                .resourceId("42")
                .changePhase(ChangePhase.PRE)
                .changeOperation(ChangeOperation.SAVE)
                .beforeRef("it.water.SomeEntity#41")
                .afterRef("it.water.SomeEntity#42")
                .build();

        // inherited fields
        assertEquals("de-1", record.recordId());
        assertEquals(RecordType.PERSISTENCE, record.recordType());
        assertEquals(now, record.timestamp());
        assertEquals(1, record.schemaVersion());
        assertEquals(0L, record.durationMillis());
        assertEquals(Outcome.SUCCESS, record.outcome());
        assertEquals("core-service", record.serviceName());
        assertEquals("Core-service", record.moduleId());
        assertEquals("node-1", record.nodeId());

        // domain-event-specific fields
        assertEquals("it.water.core.api.entity.events.PreSaveEvent", record.eventClass());
        assertEquals("it.water.SomeEntity", record.resourceType());
        assertEquals("42", record.resourceId());
        assertEquals(ChangePhase.PRE, record.changePhase());
        assertEquals(ChangeOperation.SAVE, record.changeOperation());
        assertEquals("it.water.SomeEntity#41", record.beforeRef());
        assertEquals("it.water.SomeEntity#42", record.afterRef());

        assertNotNull(record.getResourceName());
        assertTrue(record instanceof TrafficRecord);
        assertTrue(record instanceof TrafficDomainEventRecord);
    }

    @Test
    void builder_nonDetailedEvent_beforeAndAfterRefsCanBeLeftNull() {
        WaterTrafficDomainEventRecord record = WaterTrafficDomainEventRecord.builder()
                .recordId("de-2")
                .recordType(RecordType.PERSISTENCE)
                .eventClass("it.water.core.api.entity.events.PostSaveEvent")
                .resourceType("it.water.SomeEntity")
                .resourceId("7")
                .changePhase(ChangePhase.POST)
                .changeOperation(ChangeOperation.SAVE)
                .build();

        assertEquals("7", record.resourceId());
        assertNull(record.beforeRef());
        assertNull(record.afterRef());
    }

    @Test
    void builder_genericDomainEvent_changePhaseAndResourceCanBeNull() {
        WaterTrafficDomainEventRecord record = WaterTrafficDomainEventRecord.builder()
                .recordId("de-3")
                .recordType(RecordType.DOMAIN_EVENT)
                .eventClass("it.water.core.api.model.events.Event")
                .changeOperation(ChangeOperation.GENERIC)
                .build();

        assertNull(record.changePhase());
        assertNull(record.resourceType());
        assertNull(record.resourceId());
        assertEquals(ChangeOperation.GENERIC, record.changeOperation());
        assertEquals(RecordType.DOMAIN_EVENT, record.recordType());
    }
}
