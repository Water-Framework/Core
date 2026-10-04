
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

/**
 * @Author Aristide Cittadino.
 * Generic, vendor-agnostic ingress abstraction for traffic events. A {@code TrafficEventConsumer}
 * is the symmetric counterpart of {@link TrafficPublisher}: it receives inbound traffic events
 * from a source (message broker, HTTP endpoint, in-memory bus, ...) and routes them to the
 * registered {@link TrafficEventHandler}s, hiding the concrete ingress mechanism.
 * <p>
 * The consumer owns its subscription lifecycle ({@link #start()}/{@link #stop()}) and must be
 * fail-safe: transport errors are handled internally and never propagated to callers, and a
 * failing handler is isolated without stopping the ingress loop. Implementations are
 * {@code @FrameworkComponent} resolved via {@code ComponentRegistry}; N of them may coexist and
 * be selected per configuration. {@link TrafficBrokerConsumer} is one such specialization, for
 * message brokers.
 */
public interface TrafficEventConsumer extends Service {
    /**
     * Register a handler to be notified of each inbound traffic event.
     */
    void subscribe(TrafficEventHandler handler);

    /**
     * Remove a previously registered handler.
     */
    void unsubscribe(TrafficEventHandler handler);

    /**
     * Start consuming from the underlying source. Idempotent: calling it on an already started
     * consumer is a no-op.
     */
    void start();

    /**
     * Stop consuming and release the underlying source resources. Idempotent.
     */
    void stop();

    /**
     * Whether the underlying source is currently reachable/usable.
     */
    boolean isAvailable();

    /**
     * Diagnostic identifier of the concrete consumer (e.g. "memory", "http", "broker").
     */
    String consumerType();
}
