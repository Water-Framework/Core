
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

package it.water.core.api.permission;

import it.water.core.api.action.Action;
import it.water.core.api.model.ProtectedResource;
import it.water.core.api.model.Resource;
import it.water.core.api.service.Service;
import it.water.core.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino.
 * Generic Interface Component for PermissionManager.
 * This interface defines all methods able to check if a user has permissions for each actions of the
 * platform.
 */
public interface PermissionManager extends Service {
    Logger log = LoggerFactory.getLogger(PermissionManager.class.getName());

    /**
     * Checks if the user corresponding to the username has the specified roles
     *
     * @param rolesNames
     * @return
     */
    boolean userHasRoles(String username, String[] rolesNames);

    /**
     * Checks if an existing user has permissions for action of Action.
     * Moreover every user, if protected, is set as a base entity of the
     * platform.
     *
     * @param username parameter that indicates the username of entity
     * @param entity   parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the entity with  platform
     */
    boolean checkPermission(String username, Resource entity, Action action);

    /**
     * Checks if an existing user has permissions for action of Action.
     *
     * @param username parameter that indicates the username of entity
     * @param resource parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the user with  platform
     */
    boolean checkPermission(String username, Class<? extends Resource> resource,
                            Action action);

    /**
     * Checks if an existing user has permissions for action of Action.
     *
     * @param username     parameter that indicates the username of entity
     * @param resourceName parameter that indicates the resource name of action
     * @param action       interaction of the user with  platform
     */
    boolean checkPermission(String username, String resourceName, Action action);

    /**
     * @param username     parameter that indicates the username of entity
     * @param resourceName parameter that indicates the resource name of action
     * @param action       interaction of the user with  platform
     * @param entities     List of entities User must own in order to perform the action
     * @return
     */
    boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities);

    /**
     * @param username parameter that indicates the username of entity
     * @param resource parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the user with  platform
     * @param entities List of other entities User must own in order to perform the action
     * @return
     */
    boolean checkPermissionAndOwnership(String username, Resource resource, Action action, Resource... entities);

    /**
     * Checks wether resource is owned by the user
     *
     * @param user     User that should own the resource
     * @param resource Object that should be owned by the user
     * @return true if the user owns the resource
     */
    boolean checkUserOwnsResource(User user, Object resource);

    /**
     * Return the protected entity of  platform
     *
     * @param entity parameter that indicates the protected entity of
     *               platform
     * @return protected entity
     */
    static boolean isProtectedEntity(Object entity) {
        log.debug("invoking Permission Manager getProtectedEntity {}", entity.getClass().getSimpleName());
        return ((entity instanceof ProtectedResource) || (entity instanceof ProtectedEntity));
    }

    /**
     * Return the protected resource name of entity of  platform
     *
     * @param resourceName parameter that indicates the protected resource name of
     *                     entity of  platform
     * @return protected resource name of entity
     */
    static boolean isProtectedEntity(String resourceName) {
        log.debug("invoking Permission getProtectedEntity {}", resourceName);
        try {
            boolean isAssignable = ProtectedEntity.class.isAssignableFrom(Class.forName(resourceName));
            isAssignable = isAssignable || ProtectedResource.class.isAssignableFrom(Class.forName(resourceName));
            return isAssignable;
        } catch (ClassNotFoundException e) {
            log.warn(e.getMessage());
        }
        // return the most restrictive condition
        return true;
    }
}
