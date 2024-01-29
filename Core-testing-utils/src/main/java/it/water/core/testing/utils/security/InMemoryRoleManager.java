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

package it.water.core.testing.utils.security;

import it.water.core.api.permission.Role;
import it.water.core.api.permission.RoleManager;
import it.water.core.interceptors.annotations.FrameworkComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author Aristide Cittadino
 * Simple In memory Role Manager.
 */
@FrameworkComponent(priority = 0)
public class InMemoryRoleManager implements RoleManager {
    Map<Long, Set<String>> userRoles = new HashMap<>();
    Set<String> roles = new HashSet<>();

    @Override
    public Role createIfNotExists(String roleName) {
        roles.add(roleName);
        return new Role() {
            @Override
            public String getName() {
                return roleName;
            }
        };
    }

    @Override
    public boolean exists(String roleName) {
        return roles.contains(roleName);
    }

    @Override
    public boolean hasRole(long userId, String roleName) {
        return userRoles.containsKey(userId) && userRoles.get(userId).contains(roleName);
    }

    @Override
    public boolean addRole(long userId, Role role) {
        userRoles.computeIfAbsent(userId, key -> new HashSet<>());
        return userRoles.get(userId).add(role.getName());
    }

    @Override
    public boolean removeRole(long userId, Role role) {
        userRoles.computeIfAbsent(userId, key -> new HashSet<>());
        return userRoles.get(userId).remove(role.getName());
    }
}
