
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

import it.water.core.api.traffic.events.TrafficEvent;

/**
 * @Author Aristide Cittadino.
 * Callback invoked by a {@link TrafficEventConsumer} for each inbound traffic event received
 * from a source. Handlers are registered on a consumer via
 * {@link TrafficEventConsumer#subscribe(TrafficEventHandler)}. Implementations must be
 * fail-safe: an exception thrown while handling one event must be isolated and never break
 * the consumer's ingress loop nor the delivery to other handlers.
 */
@FunctionalInterface
public interface TrafficEventHandler {
    /**
     * Handle a single inbound traffic event.
     */
    void onTrafficEvent(TrafficEvent<?> event);
}
