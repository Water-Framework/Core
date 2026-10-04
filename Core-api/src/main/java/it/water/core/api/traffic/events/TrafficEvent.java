
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

package it.water.core.api.traffic.events;

import it.water.core.api.model.events.Event;
import it.water.core.api.traffic.model.TrafficRecord;

/**
 * @Author Aristide Cittadino.
 * Water event carrying a traffic record and an optional, in-process context payload.
 * The record is always present, normalized and serializable, while the payload is rich
 * and does not cross the broker as-is.
 *
 * @param <P> type of the in-process context payload
 */
public interface TrafficEvent<P> extends Event {
    TrafficRecord record();

    P payload();

    Class<P> payloadType();
}
