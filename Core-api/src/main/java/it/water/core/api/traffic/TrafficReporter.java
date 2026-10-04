
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

package it.water.core.api.traffic;

import it.water.core.api.service.Service;
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.api.traffic.model.TrafficRecord;

/**
 * @Author Aristide Cittadino.
 * Cross-cutting entry point for traffic reporting. Collects, normalizes and applies
 * policies on traffic records captured at the REST, Api, SystemApi and persistence layers,
 * then delegates transport to the configured {@link TrafficPublisher}.
 * Implementations must short-circuit when reporting is disabled.
 */
public interface TrafficReporter extends Service {
    void report(TrafficRecord record);

    void reportCall(TrafficCallRecord record);

    void reportDomainEvent(TrafficDomainEventRecord record);

    <P> void report(TrafficRecord record, P payload);

    <P> void reportCall(TrafficCallRecord record, P payload);

    <P> void reportDomainEvent(TrafficDomainEventRecord record, P payload);

    boolean isEnabled();
}
