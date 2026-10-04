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
 * Component-registration properties that discriminate the available {@link TrafficPublisher}
 * implementations, so one can be selected with a {@code ComponentFilter}.
 * <p>
 * These are COMPONENT properties, not application configuration keys: in the Spring runtime they
 * are bound to actual BEAN properties of the component, so the name must be a plain bean-property
 * identifier (no dots, and the component must expose a matching getter/setter) - same convention
 * as {@code PermissionManagerComponentProperties} and {@code ServiceDiscoveryServerProperties}.
 * The dotted {@code water.traffic.publisher.type} key lives in the application properties and
 * carries the VALUE to match against {@link #TRAFFIC_PUBLISHER_IMPLEMENTATION_PROP}.
 */
public final class TrafficPublisherComponentProperties {

    public static final String TRAFFIC_PUBLISHER_IMPLEMENTATION_PROP = "implementation";
    public static final String TRAFFIC_PUBLISHER_IN_MEMORY_IMPLEMENTATION = "memory";

    private TrafficPublisherComponentProperties() {
    }
}
