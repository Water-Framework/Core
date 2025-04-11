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
import it.water.core.api.action.ResourceAction;
import it.water.core.api.entity.owned.OwnedChildResource;
import it.water.core.api.entity.owned.OwnedResource;
import it.water.core.api.model.Resource;
import it.water.core.api.model.Role;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.PermissionManagerComponentProperties;
import it.water.core.api.role.RoleManager;
import it.water.core.api.user.UserManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.permission.action.ActionFactory;
import it.water.core.testing.utils.api.TestPermissionManager;
import jakarta.persistence.NoResultException;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Aristide Cittadino
 * Fake permission Manager which allows every action coming from username with "usernameOk" and deny others.
 * Use it just for test purpose
 */
@FrameworkComponent(priority = 0, properties = {PermissionManagerComponentProperties.PERMISSION_MANAGER_IMPLEMENTATION_PROP + "=" + PermissionManagerComponentProperties.PERMISSION_MANAGER_DEFAILT_IMPLEMENTATION}, services = {PermissionManager.class, TestPermissionManager.class})
public class InMemoryTestPermissionManager implements TestPermissionManager {
    Map<Role, Set<ResourceAction<?>>> rolePermissions = new HashMap<>();
    @Inject
    @Setter
    private RoleManager roleManager;
    //for spring tests
    @Setter
    @Getter
    private String implementation = "default";

    @Inject
    @Setter
    private UserManager userManager;

    @Override
    public void addPermissionIfNotExists(Role r, Class<? extends Resource> resourceClass, Action action) {
        rolePermissions.computeIfAbsent(r, key -> new HashSet<>());
        ResourceAction<?> resourceAction = ActionFactory.createResourceAction(resourceClass, action);
        rolePermissions.get(r).add(resourceAction);
    }

    @Override
    public boolean userHasRoles(String username, String[] rolesNames) {
        boolean hasAllRoles = false;
        for (int i = 0; i < rolesNames.length; i++) {
            boolean hasRole = roleManager.hasRole(userManager.findUser(username).getId(), rolesNames[i]);
            if (i == 0)
                hasAllRoles = hasRole;
            else
                hasAllRoles = hasAllRoles && hasRole;
        }
        return hasAllRoles;
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
        User user = null;
        try {
            user = userManager.findUser(username);
        } catch (NoResultException e) {
            log.debug("No user found with username: {}", username);
        }
        if (user == null)
            return false;
        if (user.isAdmin())
            return true;
        Set<Role> userRoles = roleManager.getUserRoles(userManager.findUser(username).getId());
        if (!userRoles.isEmpty()) {
            return userRoles.stream().anyMatch(role -> rolePermissions.containsKey(role) && rolePermissions.get(role).stream().anyMatch(resourceAction -> resourceAction.getResourceClass().getName().equalsIgnoreCase(resourceName) && resourceAction.getAction().equals(action)));
        }
        return false;
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities) {
        boolean permission = checkPermission(username, resourceName, action);
        final AtomicReference<Boolean> owned = new AtomicReference<>();
        owned.set(checkUserOwnsResource(userManager.findUser(username), resourceName));
        Arrays.stream(entities).forEach(entity -> owned.set(owned.get() && checkUserOwnsResource(userManager.findUser(username), entity)));
        return permission && owned.get();
    }

    @Override
    public boolean checkPermissionAndOwnership(String username, Resource resource, Action action, Resource... entities) {
        return checkPermissionAndOwnership(username, resource.getClass().getName(), action);
    }

    @Override
    public boolean checkUserOwnsResource(User user, Object resource) {
        if (user.isAdmin())
            return true;
        if (resource instanceof OwnedResource || resource instanceof OwnedChildResource) {
            return ((OwnedResource) resource).getOwnerUserId() == (user.getId());
        }
        return true;
    }

    @Override
    public Map<String, Map<String, Map<String, Boolean>>> entityPermissionMap(String username, Map<String, List<Long>> entityPks) {
        //todo implement
        throw new UnsupportedOperationException();
    }
}
