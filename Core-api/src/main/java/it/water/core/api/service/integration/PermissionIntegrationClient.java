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

import it.water.core.api.action.ResourceAction;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.Permission;

import java.util.Collection;
import java.util.List;

public interface PermissionIntegrationClient extends EntityIntegrationClient{
    Permission findByUserAndResource(long userId, Resource resource);

    /**
     * Find a permission by a specific user and resource name
     *
     * @param userId         user parameter
     * @param resourceName parameter required to find a resource name
     * @return Permission if found
     */
    Permission findByUserAndResourceName(long userId, String resourceName);

    /**
     * Find a permission by a specific user, resource name and resource id
     *
     * @param userId       user parameter
     * @param resourceName parameter required to find a resource name
     * @param id           parameter required to find a resource id
     * @return Permission if found
     */
    Permission findByUserAndResourceNameAndResourceId(long userId, String resourceName, long id);

    /**
     * Find a permission by a specific role and resource name
     *
     * @param roleId       parameter required to find role by roleId
     * @param resourceName parameter required to find a resource name
     * @return Permission if found
     */
    Permission findByRoleAndResourceName(long roleId, String resourceName);

    /**
     * Find a permission by a specific role and resource name
     *
     * @param roleId parameter required to find role by roleId
     * @return Permission if found
     */
    Collection<Permission> findByRole(long roleId);

    /**
     * Find a permission by a specific role, resource name and resource id
     *
     * @param roleId       parameter required to find role by roleId
     * @param resourceName parameter required to find a resource name
     * @param resourceId   parameter required to find a resource id
     * @return Permission if found
     */
    Permission findByRoleAndResourceNameAndResourceId(long roleId, String resourceName, long resourceId);

    /**
     * @param roleId
     * @param actions List actions to add as permissions
     */
    void checkOrCreatePermissions(long roleId, List<ResourceAction> actions);

    /**
     * @param roleId
     * @param entityId
     * @param actions
     */
    void checkOrCreatePermissionsSpecificToEntity(long roleId, long entityId, List<ResourceAction> actions);

    /**
     * Verify if exist a permission specific to entity
     *
     * @param resourceName parameter required to find a resource name
     * @param resourceId   parameter required to find a resource id
     * @return true if exist a specific permission to this entity, false otherwise
     */
    boolean permissionSpecificToEntityExists(String resourceName, long resourceId);
}
