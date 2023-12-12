/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.core.api.permission;

import it.water.core.api.action.Action;
import it.water.core.api.model.Resource;
import it.water.core.api.service.Service;

public interface PermissionUtil extends Service {

    /**
     * Returns true if user has at least one role listed in the rolesNames array
     *
     * @param username
     * @param rolesNames
     * @return
     */
    boolean userHasRoles(String username, String[] rolesNames);

    /**
     * @param o      resource
     * @param action Action
     * @return true if Logged User has permission to do the action on the specified entity
     */
    boolean checkPermission(Object o, Action action);


    /**
     * @param action Action
     * @return true if Logged User has permission to do the action generically
     * NOTE: this method do not check if the user can modify (owns) the entity, so it's generic permission
     * that should be used carefully
     */
    boolean checkPermission(String resourceName, Action action);

    /**
     * @param resourceName Resource Name
     * @param action       Action
     * @param entities     List of entities that users must own in order allow the action
     * @return NOTE: this method checks whether logged user can perform a generic action but at the same time
     * the user itself must own other resources.
     * For an example see @See AreaServiceImpl
     */
    boolean checkPermissionAndOwnership(String resourceName, Action action, Resource... entities);

    /**
     * @param o        the entity on which action should be performed
     * @param action   Action
     * @param entities List of entities that users must own in order allow the action
     * @return NOTE: this method checks whether logged user can perform a generic action but at the same time
     * the user itself must own other resources.
     * For an example see @See AreaServiceImpl
     */
    boolean checkPermissionAndOwnership(Object o, Action action, Resource... entities);
}
