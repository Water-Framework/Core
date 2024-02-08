
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

package it.water.core.security.util;

import it.water.core.api.action.Action;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.PermissionUtil;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino
 * Class fot simplify interaction with permission manager
 */
@FrameworkComponent(services = PermissionUtil.class)
public class WaterPermissionUtilImpl implements PermissionUtil {
    private static final Logger log = LoggerFactory.getLogger(WaterPermissionUtilImpl.class);

    @Inject
    @Setter
    private Runtime waterRuntime;

    @Inject
    @Setter
    private PermissionManager pm;

    /**
     * Returns true if user has at least one role listed in the rolesNames array
     *
     * @param username
     * @param rolesNames
     * @return
     */
    public boolean userHasRoles(String username, String[] rolesNames) {
        log.debug("Checking {} has {} roles", username, rolesNames);
        if (pm == null)
            return false;
        return pm.userHasRoles(username, rolesNames);
    }

    /**
     * @param o      resource
     * @param action Action
     * @return true if Logged User has permission to do the action on the specified entity
     */
    public boolean checkPermission(Object o,
                                   Action action) {
        log.debug("Checking permission resource {} has {} action", o, action);
        if (!PermissionManager.isProtectedEntity(o))
            return true;
        //entity is protected but no permission manager has been found
        if ((pm == null)) {
            return false;
        }
        Resource entity = (Resource) o;
        return pm.checkPermission(waterRuntime.getSecurityContext().getLoggedUsername(), entity, action);
    }

    /**
     * @param action Action
     * @return true if Logged User has permission to do the action generically
     * NOTE: this method do not check if the user can modify (owns) the entity, so it's generic permission
     * that should be used carefully
     */
    public boolean checkPermission(String resourceName,
                                   Action action) {
        log.debug("Checking permission resource {} has {} action", resourceName, action);
        if (!PermissionManager.isProtectedEntity(resourceName))
            return true;
        //entity is protected but no permission manager has been found
        if ((pm == null)) {
            return false;
        }
        return pm.checkPermission(waterRuntime.getSecurityContext().getLoggedUsername(), resourceName, action);
    }

    /**
     * @param resourceName Resource Name
     * @param action       Action
     * @param entities     List of entities that users must own in order allow the action
     * @return NOTE: this method checks whether logged user can perform a generic action but at the same time
     * the user itself must own other resources.
     * For an example see @See AreaServiceImpl
     */
    public boolean checkPermissionAndOwnership(String resourceName,
                                               Action action, Resource... entities) {
        log.debug("Checking permission resource {} has {} action with owned entities {}", resourceName, action, entities);
        if (!PermissionManager.isProtectedEntity(resourceName))
            return true;
        //entity is protected but no permission manager has been found
        if ((pm == null)) {
            return false;
        }
        return pm.checkPermissionAndOwnership(waterRuntime.getSecurityContext().getLoggedUsername(), resourceName, action, entities);
    }

    /**
     * @param o        the entity on which action should be performed
     * @param action   Action
     * @param entities List of entities that users must own in order allow the action
     * @return NOTE: this method checks whether logged user can perform a generic action but at the same time
     * the user itself must own other resources.
     * For an example see @See AreaServiceImpl
     */
    public boolean checkPermissionAndOwnership(Object o, Action action, Resource... entities) {
        log.debug("Checking permission resource {} has {} action with owned entities {}", o, action, entities);
        if (!PermissionManager.isProtectedEntity(o))
            return true;
        //entity is protected but no permission manager has been found
        if ((pm == null)) {
            return false;
        }
        Resource entity = (Resource) o;
        return pm.checkPermissionAndOwnership(waterRuntime.getSecurityContext().getLoggedUsername(), entity, action, entities);
    }
}
