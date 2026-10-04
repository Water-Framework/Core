
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

package it.water.core.api.model.events;

import it.water.core.api.model.Resource;


/**
 * @Author Aristide Cittadino.
 * This interface represents an event lister for pre/post events.
 * Every object which implements it will be notified on specific events.
 * <p>
 * <b>The resource is the LIVE instance, not a copy.</b> On a {@code Pre*} event the listener
 * receives the very same object the emitter is about to hand to the repository, so any mutation
 * applied here ends up persisted; on a {@code Post*} event it receives the instance the emitter
 * just wrote. This is intentional (it lets a listener enrich an entity before the write) but it
 * makes every listener a potential writer: mutate the resource ONLY when that is the explicit
 * purpose of the listener, and never as a side effect of inspecting it.
 * <p>
 * <b>Delivery is synchronous and outside the persistence transaction</b>, and a listener that
 * throws neither stops the other listeners nor rolls back the operation. See the reference
 * producer implementation for the full contract and its consequences.
 * <p>
 * <b>Delivery is not type-filtered.</b> Because of generics erasure a producer resolves every
 * registered listener regardless of its {@code <T extends Resource>} bound, so implementations
 * must check the type of the resource they get instead of assuming it.
 */
public interface ApplicationEventListener<T extends Resource> {
    void consumerEvent(T resource, Event event);

    void consumerDetailedEvent(T beforeResource, T afterResource, Event event);
}
