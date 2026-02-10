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

import it.water.core.api.model.AssetTagResource;

/**
 * This interface defines the integration client for asset tag management.
 *
 * It supports different implementations:
 *  - local - using direct service calls
 *  - remote - using remote REST APIs
 */
public interface AssetTagIntegrationClient extends EntityIntegrationClient {

    /**
     * Find an AssetTagResource by its parameters
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id
     * @return the found AssetTagResource or null
     */
    AssetTagResource findAssetTagResource(String resourceName, long resourceId, long tagId);

    /**
     * Add a tag to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id to associate
     */
    void addAssetTag(String resourceName, long resourceId, long tagId);

    /**
     * Add multiple tags to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagIds array of tag ids to associate
     */
    void addAssetTags(String resourceName, long resourceId, long[] tagIds);

    /**
     * Find all tags associated with a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @return array of tag ids
     */
    long[] findAssetTags(String resourceName, long resourceId);

    /**
     * Remove a tag from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id to remove
     */
    void removeAssetTag(String resourceName, long resourceId, long tagId);

    /**
     * Remove multiple tags from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagIds array of tag ids to remove
     */
    void removeAssetTags(String resourceName, long resourceId, long[] tagIds);
}
