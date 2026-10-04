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

import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficEvent}: no Water framework harness required, this is a
 * simple immutable event wrapper with a hand-written constructor (not Lombok-generated).
 */
class WaterTrafficEventTest {

    @Test
    void constructor_withPayload_exposesRecordPayloadAndPayloadType() {
        TrafficRecord record = WaterTrafficRecord.builder().recordId("e1").recordType(RecordType.API).build();

        WaterTrafficEvent<String> event = new WaterTrafficEvent<>(record, "ctx-payload", String.class);

        assertSame(record, event.record());
        assertEquals("ctx-payload", event.payload());
        assertEquals(String.class, event.payloadType());
        assertTrue(event instanceof TrafficEvent);
    }

    @Test
    void constructor_withNullPayload_payloadAndPayloadTypeAreNull() {
        TrafficRecord record = WaterTrafficRecord.builder().recordId("e2").recordType(RecordType.API).build();

        WaterTrafficEvent<Object> event = new WaterTrafficEvent<>(record, null, null);

        assertSame(record, event.record());
        assertNull(event.payload());
        assertNull(event.payloadType());
    }
}
