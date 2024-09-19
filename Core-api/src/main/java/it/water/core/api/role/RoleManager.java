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

package it.water.core.api.role;

import it.water.core.api.model.Role;
import it.water.core.api.service.Service;

import java.util.Set;

/**
 * @Author Aristide Cittadino
 * Class which has the ownership of managing roles inside the platform.
 */
public interface RoleManager extends Service  {
    /**
     * Creates a role if not exists
     *
     * @param roleName
     * @return
     */
    Role createIfNotExists(String roleName);

    /**
     * @param roleName
     * @return true if a Role with the given role name exists
     */
    boolean exists(String roleName);

    /**
     * @param userId   the user id
     * @param roleName role name
     * @return true if the user ha the specified role name
     */
    boolean hasRole(long userId, String roleName);

    /**
     * Return all user roles
     * @param userId
     * @return
     */
    Set<Role> getUserRoles(long userId);

    /**
     * Adds a new role to the specified user id
     *
     * @param userId
     * @param role
     * @return
     */
    boolean addRole(long userId, Role role);

    /**
     *
     * @param roleName role name which must be found
     * @return
     */
    Role getRole(String roleName);

    /**
     * Removes a role from a user
     *
     * @param userId
     * @param role
     * @return
     */
    boolean removeRole(long userId, Role role);
}
