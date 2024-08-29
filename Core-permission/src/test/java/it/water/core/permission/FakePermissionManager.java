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

import it.water.core.api.action.Action;
import it.water.core.api.model.Resource;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.Role;

import java.util.List;
import java.util.Map;

public class FakePermissionManager implements PermissionManager {
    @Override
    public boolean userHasRoles(String username, String[] rolesNames) {
        return false;
    }

    @Override
    public void addPermissionIfNotExists(Role r, Class<? extends Resource> resourceClass, Action action) {

    }

    @Override
    public boolean checkPermission(String username, Resource entity, Action action) {
        return false;
    }

    @Override
    public boolean checkPermission(String username, Class<? extends Resource> resource, Action action) {
        return false;
    }

    @Override
    public boolean checkPermission(String username, String resourceName, Action action) {
        return false;
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities) {
        return false;
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, Resource resource, Action action, Resource... entities) {
        return false;
    }

    @Override
    public boolean checkUserOwnsResource(User user, Object resource) {
        return false;
    }

    @Override
    public Map<String, Map<String, Map<String, Boolean>>> entityPermissionMap(String username, Map<String, List<Long>> entityPks) {
        throw new UnsupportedOperationException();
    }
}
