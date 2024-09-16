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

import it.water.core.api.model.Role;
import it.water.core.api.role.RoleManager;
import it.water.core.api.service.integration.RoleIntegrationClient;
import it.water.core.interceptors.annotations.FrameworkComponent;

import java.util.*;

/**
 * @Author Aristide Cittadino
 * Simple In memory Role Manager.
 */
@FrameworkComponent(priority = 0,services = {RoleManager.class,RoleIntegrationClient.class})
public class InMemoryTestRoleManager implements RoleManager, RoleIntegrationClient {
    private static long roleCount = 1;
    Map<Long, Set<Role>> userRoles = new HashMap<>();
    Set<Role> roles = new HashSet<>();

    @Override
    public Role createIfNotExists(String roleName) {
        Role r = new Role() {
            private long id = roleCount++;
            @Override
            public long getId() {
                return id;
            }

            @Override
            public String getName() {
                return roleName;
            }

            @Override
            public int hashCode() {
                return this.getName().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Role))
                    return false;
                return ((Role) obj).getName().equals(getName());
            }
        };
        roles.add(r);
        return r;
    }

    @Override
    public Role getRole(String roleName) {
        Optional<Role> roleOpt = roles.stream().filter(role -> role.getName().equals(roleName)).findFirst();
        if (roleOpt.isPresent())
            return roleOpt.get();
        return null;
    }

    @Override
    public boolean exists(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    @Override
    public boolean hasRole(long userId, String roleName) {
        return userRoles.containsKey(userId) && userRoles.get(userId).stream().anyMatch(role -> role.getName().equals(roleName));
    }

    @Override
    public Set<Role> getUserRoles(long userId) {
        if (!this.userRoles.containsKey(userId))
            return Collections.emptySet();
        return Collections.unmodifiableSet(this.userRoles.get(userId));
    }

    @Override
    public boolean addRole(long userId, Role role) {
        userRoles.computeIfAbsent(userId, key -> new HashSet<>());
        return userRoles.get(userId).add(role);
    }

    @Override
    public boolean removeRole(long userId, Role role) {
        userRoles.computeIfAbsent(userId, key -> new HashSet<>());
        return userRoles.get(userId).remove(role);
    }

    @Override
    public Collection<Role> fetchUserRoles(long userId) {
        return getUserRoles(userId);
    }
}
