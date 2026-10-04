
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

/**
 * @Author Aristide Cittadino.
 * Broker-oriented specialization of {@link TrafficEventConsumer}, symmetric to
 * {@link TrafficBrokerPublisher}. Consumes traffic from a message broker (Kafka, RabbitMQ, ...)
 * by subscribing to a logical topic within a consumer group, deserializing each message into a
 * {@code TrafficEvent} and routing it to the registered handlers. The vendor dependency stays
 * confined to the concrete adapter implementation; this contract only exposes broker-neutral
 * primitives, so a new broker is added as a new adapter without touching the rest of the pipeline.
 */
public interface TrafficBrokerConsumer extends TrafficEventConsumer {
    /**
     * Bind this consumer to a logical topic/channel within a consumer group before {@link #start()}.
     */
    void subscribe(String topic, String group);

    /**
     * Broker connection state (used, for instance, by reconnection/health logic).
     */
    boolean isConnected();

    /**
     * Diagnostic identifier of the concrete broker (e.g. "kafka").
     */
    String brokerType();

    /**
     * A broker consumer is available when it is connected to the broker.
     */
    @Override
    default boolean isAvailable() {
        return isConnected();
    }

    /**
     * Broker consumers report a uniform "broker" consumer type; the concrete vendor is exposed
     * by {@link #brokerType()}.
     */
    @Override
    default String consumerType() {
        return "broker";
    }
}
