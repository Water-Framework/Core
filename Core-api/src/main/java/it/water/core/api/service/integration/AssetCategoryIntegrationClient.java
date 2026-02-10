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

package it.water.core.api.service.integration;

import it.water.core.api.model.AssetCategoryResource;

/**
 * This interface defines the integration client for asset category management.
 *
 * It supports different implementations:
 *  - local - using direct service calls
 *  - remote - using remote REST APIs
 */
public interface AssetCategoryIntegrationClient extends EntityIntegrationClient {

    /**
     * Find an AssetCategoryResource by its parameters
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoryId the category id
     * @return the found AssetCategoryResource or null
     */
    AssetCategoryResource findAssetCategoryResource(String resourceName, long resourceId, long categoryId);

    /**
     * Add a category to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoryId the category id to associate
     */
    void addAssetCategory(String resourceName, long resourceId, long categoryId);

    /**
     * Add multiple categories to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoriesId array of category ids to associate
     */
    void addAssetCategories(String resourceName, long resourceId, long[] categoriesId);

    /**
     * Find all categories associated with a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @return array of category ids
     */
    long[] findAssetCategories(String resourceName, long resourceId);

    /**
     * Remove a category from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoryId the category id to remove
     */
    void removeAssetCategory(String resourceName, long resourceId, long categoryId);

    /**
     * Remove multiple categories from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoriesId array of category ids to remove
     */
    void removeAssetCategories(String resourceName, long resourceId, long[] categoriesId);
}
