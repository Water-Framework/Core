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

package it.water.core.permission;

import it.water.core.api.permission.Role;
import it.water.core.api.permission.RoleManager;

import java.util.Collections;
import java.util.Set;

public class FakeRoleManager implements RoleManager {
    private static Role fakeRole = new Role() {
        @Override
        public String getName() {
            return "FAKEROLE";
        }
    };

    @Override
    public Role createIfNotExists(String roleName) {
        return fakeRole;
    }

    @Override
    public Set<Role> getUserRoles(long userId) {
        return Collections.singleton(fakeRole);
    }

    @Override
    public Role getRole(String roleName) {
        return fakeRole;
    }

    @Override
    public boolean exists(String roleName) {
        return false;
    }

    @Override
    public boolean hasRole(long userId, String roleName) {
        return false;
    }

    @Override
    public boolean addRole(long userId, Role role) {
        return false;
    }

    @Override
    public boolean removeRole(long userId, Role role) {
        return false;
    }
}
