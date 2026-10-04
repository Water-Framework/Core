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
 * Specialization of {@link TrafficRecord} for domain events. Unlike a
 * {@link TrafficCallRecord}, a domain event is a fact that has already happened: it has no
 * direction and no meaningful duration, and it is described by the resource it affects and by
 * the kind of change it carries.
 * <p>
 * {@link #beforeRef()} and {@link #afterRef()} are lightweight REFERENCES to the resource state
 * (never the payload itself, see ADR-4) and are populated only for detailed events, which carry
 * both a before and an after image.
 */
public interface TrafficDomainEventRecord extends TrafficRecord {
    String eventClass();

    String resourceType();

    String resourceId();

    ChangePhase changePhase();

    ChangeOperation changeOperation();

    String beforeRef();

    String afterRef();
}
