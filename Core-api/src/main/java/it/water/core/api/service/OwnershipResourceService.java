
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

package it.water.core.api.service;

/**
 * @Author Aristide Cittadino.
 * This interface helps to manage owned resource on service classes.
 * It helps to tell the system how to retrieve the parent field of an owned entity based on
 * dotted notation.
 */
public interface OwnershipResourceService extends Service {
    String getOwnerFieldPath();
}
