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

/**
 * Interface representing the association between a resource (any entity) and a category.
 * This allows generic categorization of any entity in the system.
 */
public interface AssetCategoryResource extends Resource {

    /**
     * @return the resource class name (typically entity.getClass().getName())
     */
    String getResourceName();

    /**
     * @return the resource primary key
     */
    long getResourceId();

    /**
     * @return the category id associated with this resource
     */
    long getCategoryId();
}
