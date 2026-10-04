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
import it.water.core.api.traffic.model.TrafficDomainEventRecord;

/**
 * @Author Aristide Cittadino.
 * Default implementation of {@link TrafficDomainEvent}: carries the normalized, serializable
 * {@link TrafficDomainEventRecord} plus an optional in-process context payload (typically the
 * affected resource). The payload never crosses a broker (ADR-12), only the record does.
 *
 * @param <P> type of the in-process context payload
 */
public class WaterTrafficDomainEvent<P> extends WaterTrafficEvent<P> implements TrafficDomainEvent<P> {

    public WaterTrafficDomainEvent(TrafficDomainEventRecord record, P payload, Class<P> payloadType) {
        super(record, payload, payloadType);
    }

    @Override
    public TrafficDomainEventRecord record() {
        return (TrafficDomainEventRecord) super.record();
    }
}
