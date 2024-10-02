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

package it.water.core.api.model;

import it.water.core.api.entity.owned.OwnedResource;

/**
 * @Author Aristide Cittadino
 * Interface which represents the concpet of a shared entity.
 * Users can share entities in order to let other user perform action (according to the permission system) on it.
 *
 * Every Shared Resource must be an owned resource, so that a user can share a resource only if he owns it.
 */
public interface SharedEntity extends OwnedResource {
}
