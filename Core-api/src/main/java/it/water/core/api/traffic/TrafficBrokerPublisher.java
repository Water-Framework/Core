
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
 * Broker-oriented specialization of {@link TrafficPublisher}. Delivers traffic to a message
 * broker (Kafka, RabbitMQ, ...) by serializing the event and sending it to a logical topic
 * with a partition key. The vendor dependency stays confined to the concrete adapter
 * implementation; this contract only exposes broker-neutral transport primitives, so a new
 * broker is added as a new adapter without touching the rest of the traffic pipeline.
 */
public interface TrafficBrokerPublisher extends TrafficPublisher {
    /**
     * Non-blocking send of an already serialized message to a logical topic/channel.
     */
    void send(String topic, String partitionKey, byte[] payload);

    /**
     * Broker connection state (used, for instance, by a circuit breaker around delivery).
     */
    boolean isConnected();

    /**
     * Diagnostic identifier of the concrete broker (e.g. "kafka").
     */
    String brokerType();

    /**
     * A broker publisher is available when it is connected to the broker.
     */
    @Override
    default boolean isAvailable() {
        return isConnected();
    }

    /**
     * Broker publishers report a uniform "broker" publisher type; the concrete vendor is
     * exposed by {@link #brokerType()}.
     */
    @Override
    default String publisherType() {
        return "broker";
    }
}
