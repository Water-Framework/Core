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

import it.water.core.api.traffic.events.TrafficDomainEvent;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficDomainEvent}: no Water framework harness required.
 * Mirrors {@link WaterTrafficCallEventTest}, additionally verifying the covariant
 * {@link WaterTrafficDomainEvent#record()} override returns the specialized
 * {@link TrafficDomainEventRecord} type rather than just the base {@code TrafficRecord}.
 */
class WaterTrafficDomainEventTest {

    private TrafficDomainEventRecord sampleRecord() {
        return WaterTrafficDomainEventRecord.builder()
                .recordId("de-evt-1")
                .recordType(RecordType.PERSISTENCE)
                .eventClass("it.water.core.api.entity.events.PreSaveEvent")
                .resourceType("it.water.SomeEntity")
                .resourceId("1")
                .changePhase(ChangePhase.PRE)
                .changeOperation(ChangeOperation.SAVE)
                .build();
    }

    @Test
    void constructor_withPayload_exposesRecordPayloadAndPayloadType() {
        TrafficDomainEventRecord record = sampleRecord();

        WaterTrafficDomainEvent<String> event = new WaterTrafficDomainEvent<>(record, "affected-resource", String.class);

        assertSame(record, event.record());
        assertEquals("affected-resource", event.payload());
        assertEquals(String.class, event.payloadType());
        assertTrue(event instanceof TrafficEvent);
        assertTrue(event instanceof TrafficDomainEvent);
    }

    @Test
    void constructor_withNullPayload_payloadAndPayloadTypeAreNull() {
        TrafficDomainEventRecord record = sampleRecord();

        WaterTrafficDomainEvent<Object> event = new WaterTrafficDomainEvent<>(record, null, null);

        assertSame(record, event.record());
        assertNull(event.payload());
        assertNull(event.payloadType());
    }

    @Test
    void record_returnsSpecializedTrafficDomainEventRecordType() {
        TrafficDomainEventRecord record = sampleRecord();
        WaterTrafficDomainEvent<Object> event = new WaterTrafficDomainEvent<>(record, null, null);

        TrafficDomainEventRecord returned = event.record();

        assertEquals(ChangeOperation.SAVE, returned.changeOperation());
        assertEquals(ChangePhase.PRE, returned.changePhase());
        assertEquals("it.water.SomeEntity", returned.resourceType());
    }
}
