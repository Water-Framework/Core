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

import it.water.core.api.action.Action;
import it.water.core.api.model.Resource;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;

/**
 * @Author Aristide Cittadino
 * Fake permission Manager which allows every action coming from username with "usernameOk" and deny others.
 * Use it just for test purpose
 */
public class FakePermissionManager implements PermissionManager {
    @Override
    public boolean userHasRoles(String username, String[] rolesNames) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkPermission(String username, Resource entity, Action action) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkPermission(String username, Class<? extends Resource> resource, Action action) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkPermission(String username, String resourceName, Action action) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, Resource resource, Action action, Resource... entities) {
        return checkByTestUsername(username);
    }

    @Override
    public boolean checkUserOwnsResource(User user, Object resource) {
        return checkByTestUsername(user.getUsername());
    }

    private boolean checkByTestUsername(String username) {
        return username.equalsIgnoreCase("usernameOk");
    }
}
