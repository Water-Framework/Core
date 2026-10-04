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

import it.water.core.api.traffic.events.TrafficCallEvent;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficCallRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain POJO tests for {@link WaterTrafficCallEvent}: no Water framework harness required.
 */
class WaterTrafficCallEventTest {

    @Test
    void constructor_withPayload_exposesCallRecordPayloadAndPayloadType() {
        TrafficCallRecord callRecord = WaterTrafficCallRecord.builder()
                .recordId("ce1")
                .recordType(RecordType.REST)
                .operation("op")
                .build();

        WaterTrafficCallEvent<Integer> event = new WaterTrafficCallEvent<>(callRecord, 7, Integer.class);

        assertSame(callRecord, event.record());
        assertEquals(Integer.valueOf(7), event.payload());
        assertEquals(Integer.class, event.payloadType());
        assertTrue(event instanceof TrafficEvent);
        assertTrue(event instanceof TrafficCallEvent);
    }

    @Test
    void constructor_withNullPayload_payloadAndPayloadTypeAreNull() {
        TrafficCallRecord callRecord = WaterTrafficCallRecord.builder()
                .recordId("ce2")
                .recordType(RecordType.PERSISTENCE)
                .operation("op2")
                .build();

        WaterTrafficCallEvent<Object> event = new WaterTrafficCallEvent<>(callRecord, null, null);

        assertSame(callRecord, event.record());
        assertNull(event.payload());
        assertNull(event.payloadType());
    }
}
