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

import java.util.*;

/**
 * @Author Aristide Cittadino
 * This class implements a permission manager for test purpose.
 * The developer can setup permission in order to configure it at runtime during the test phase.
 */
public class TestPermissionManager implements PermissionManager {

    //Roles map
    private Map<String, Set<String>> userRoles;

    //Permissions Map<ResourceClassAsStr,Map<Username,Set<Action>>>
    private Map<String, Map<String, Set<Action>>> permissions;

    public TestPermissionManager() {
        userRoles = new HashMap<>();
        permissions = new HashMap<>();
    }

    public TestPermissionManager withPermission(String user, Resource entity, Action action) {
        if (!permissions.containsKey(entity.getResourceName()))
            permissions.put(entity.getResourceName(), new HashMap<>());
        if (!permissions.get(entity.getResourceName()).containsKey(user))
            permissions.get(entity.getResourceName()).put(user, new HashSet<>());
        permissions.get(entity.getResourceName()).get(user).add(action);
        return this;
    }

    public void removePermission(String user, Resource entity, Action action) {
        if (permissions.containsKey(entity.getResourceName()) && permissions.get(entity.getResourceName()).containsKey(user))
            permissions.get(entity.getResourceName()).get(user).remove(action);
    }

    public TestPermissionManager withUserRole(String username, String role) {
        userRoles.computeIfAbsent(username, key -> new HashSet<>());
        userRoles.get(username).add(role);
        return this;
    }

    public void removeRole(String username, String role) {
        if (userRoles.containsKey(username)) {
            userRoles.get(username).remove(role);
        }
    }

    @Override
    public boolean userHasRoles(String username, String[] rolesNames) {
        if (userRoles.containsKey(username)) {
            return Arrays.stream(rolesNames).allMatch(role -> userRoles.get(username).contains(role));
        }
        return false;
    }

    @Override
    public boolean checkPermission(String username, Resource entity, Action action) {
        return checkPermission(username, entity.getClass(), action);
    }

    @Override
    public boolean checkPermission(String username, Class<? extends Resource> resource, Action action) {
        return checkPermission(username, resource.getName(), action);
    }

    @Override
    public boolean checkPermission(String username, String resourceName, Action action) {
        if (permissions.containsKey(resourceName) && permissions.get(resourceName).containsKey(username))
            return permissions.get(resourceName).get(username).contains(action);
        return false;
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities) {
        //todo check how to do it
        return false;
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, Resource resource, Action action, Resource... entities) {
        //todo check how to do it
        return false;
    }

    @Override
    public boolean checkUserOwnsResource(User user, Object resource) {
        //todo check how to do it
        return false;
    }
}
