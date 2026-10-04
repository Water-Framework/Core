
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
import it.water.core.api.traffic.events.TrafficEvent;

/**
 * @Author Aristide Cittadino.
 * Generic, vendor-agnostic egress abstraction for traffic notification. A
 * {@code TrafficPublisher} notifies normalized traffic events to a destination, hiding the
 * concrete delivery mechanism (in-memory dispatch, log, HTTP/webhook, message broker, ...).
 * The {@link #publish(TrafficEvent)} operation is non-blocking and fire-and-forget: overflow
 * or transport errors must be accounted as drops and never propagated as exceptions to the
 * caller.
 * <p>
 * This is the single pluggable egress point of the traffic pipeline: the {@code TrafficReporter}
 * delegates the actual delivery to the configured {@code TrafficPublisher}. Implementations are
 * {@code @FrameworkComponent} resolved via {@code ComponentRegistry} (one active, selected by
 * configuration). {@link TrafficBrokerPublisher} is one such specialization, for message
 * brokers; N further implementations may coexist and be selected per configuration.
 */
public interface TrafficPublisher extends Service {
    /**
     * Non-blocking, fire-and-forget notification of a traffic event. The event carries the
     * normalized {@link it.water.core.api.traffic.model.TrafficRecord} and an optional,
     * in-process context payload. Overflow or transport errors must be accounted as drops
     * and never surface as exceptions to the caller.
     */
    void publish(TrafficEvent<?> event);

    /**
     * Whether the underlying destination is currently reachable/usable.
     */
    boolean isAvailable();

    /**
     * Diagnostic identifier of the concrete publisher (e.g. "memory", "log", "broker").
     */
    String publisherType();
}
