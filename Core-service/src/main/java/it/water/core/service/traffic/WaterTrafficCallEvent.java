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
import it.water.core.api.traffic.model.TrafficCallRecord;

/**
 * @Author Aristide Cittadino.
 * Default implementation of {@link TrafficCallEvent} for REST and service-to-service calls.
 * Narrows {@link #record()} to a {@link TrafficCallRecord}. Both {@code payload} and
 * {@code payloadType} may be null when no rich context is attached.
 *
 * @param <P> type of the in-process context payload
 */
public class WaterTrafficCallEvent<P> implements TrafficCallEvent<P> {

    private final TrafficCallRecord record;
    private final transient P payload;
    private final Class<P> payloadType;

    public WaterTrafficCallEvent(TrafficCallRecord record, P payload, Class<P> payloadType) {
        this.record = record;
        this.payload = payload;
        this.payloadType = payloadType;
    }

    @Override
    public TrafficCallRecord record() {
        return record;
    }

    @Override
    public P payload() {
        return payload;
    }

    @Override
    public Class<P> payloadType() {
        return payloadType;
    }
}
