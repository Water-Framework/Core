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
 * Kind of change carried by a domain event. {@code SAVE}, {@code UPDATE} and {@code REMOVE} map
 * the Water CRUD event hierarchy ({@code Pre/PostSaveEvent}, {@code Pre/PostUpdateEvent} and their
 * detailed variants, {@code Pre/PostRemoveEvent}); {@code GENERIC} covers any other domain
 * {@code Event} that does not describe a CRUD transition.
 */
public enum ChangeOperation {
    SAVE, UPDATE, REMOVE, GENERIC
}
