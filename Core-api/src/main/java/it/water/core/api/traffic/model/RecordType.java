
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

package it.water.core.api.traffic.model;

/**
 * @Author Aristide Cittadino.
 * Discriminator of the application layer a {@link TrafficRecord} was captured at:
 * incoming REST calls, Api service invocations, SystemApi service invocations,
 * persistence (repository) operations and non-CRUD domain events.
 * <p>
 * Persistence is not directly interceptable in Water, so it is observed through the CRUD
 * domain events emitted around it: a {@code Pre/PostCrudEvent} is therefore classified as
 * {@link #PERSISTENCE}, while any other domain {@code Event} is classified as
 * {@link #DOMAIN_EVENT}.
 */
public enum RecordType {
    REST, API, SYSTEM_API, PERSISTENCE, DOMAIN_EVENT
}
