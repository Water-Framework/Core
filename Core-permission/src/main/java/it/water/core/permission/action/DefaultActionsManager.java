
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

package it.water.core.permission.action;

import it.water.core.api.action.ActionList;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.Role;
import it.water.core.api.permission.RoleManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.permission.annotations.DefaultRoleAccess;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;


/**
 * @Author Aristide Cittadino.
 * Every action must be registered as a component inside the registry.
 * StandardActionsManager provide registration capabilities.
 */
@FrameworkComponent(priority = 1)
public class DefaultActionsManager implements it.water.core.api.action.ActionsManager {
    protected Logger log = LoggerFactory.getLogger(DefaultActionsManager.class.getName());

    //Actions for each resource
    private Map<String, ActionList<? extends Resource>> actionsRegistry;

    @Inject
    @Setter
    private RoleManager roleManager;
    @Inject
    @Setter
    private PermissionManager permissionManager;

    public DefaultActionsManager() {
        actionsRegistry = new HashMap<>();
    }

    /**
     * Registers a list of actions that have to be registered as components
     */
    public <T extends Resource> void registerActions(Class<T> resourceClass) {
        log.debug("Invoking registerActions of {}", resourceClass.getName());
        Optional<Annotation> accessControlAnnotationOptional = Arrays.stream(resourceClass.getDeclaredAnnotations()).filter(AccessControl.class::isInstance).findAny();
        if (accessControlAnnotationOptional.isPresent()) {
            AccessControl accessControlAnnotation = (AccessControl) accessControlAnnotationOptional.get();
            String[] availableActions = accessControlAnnotation.availableActions();
            DefaultRoleAccess[] defaultRoleAccesses = accessControlAnnotation.rolesPermissions();
            //Registers all available actions
            registerActions(resourceClass, availableActions);
            //Define permission for default roles
            addDefaultRoles(resourceClass, defaultRoleAccesses);
        }
    }

    /**
     * @param resourceClass
     * @param availableActions
     */
    private <T extends Resource> void registerActions(Class<T> resourceClass, String[] availableActions) {
        ActionList<?> actionList = ActionFactory.createEmptyActionList(resourceClass);
        for (int i = 0; i < availableActions.length; i++) {
            actionList.addAction(ActionFactory.createGenericAction(resourceClass, availableActions[i], (long) Math.pow(2, i)));
        }
        if (!actionList.getList().isEmpty() && !actionsRegistry.containsKey(resourceClass.getName())) {
            log.info("Registering actions {} for Protected Resource: {}", actionList, resourceClass.getName());
            actionsRegistry.put(resourceClass.getName(), actionList);
        }
    }

    /**
     * @param resourceClass
     * @param defaultRoleAccesses
     */
    private <T extends Resource> void addDefaultRoles(Class<T> resourceClass, DefaultRoleAccess[] defaultRoleAccesses) {
        if (this.roleManager != null) {
            Arrays.stream(defaultRoleAccesses).forEach(defaultRoleAccess -> {
                Role r = this.roleManager.createIfNotExists(defaultRoleAccess.roleName());
                if (this.permissionManager != null) {
                    Arrays.stream(defaultRoleAccess.actions()).forEach(actionName -> {
                        if (this.actionsRegistry.containsKey(resourceClass.getName()) && this.actionsRegistry.get(resourceClass.getName()).containsActionName(actionName)) {
                            this.permissionManager.addPermissionIfNotExists(r, resourceClass,this.actionsRegistry.get(resourceClass.getName()).getAction(actionName));
                        }
                    });
                }
            });
        }
    }

    /**
     * Unregisters an action that have to be registered as components
     */
    public <T extends Resource> void unregisterActions(Class<T> resourceClass) {
        log.debug("Invoking registerActions of {}", resourceClass);
        this.actionsRegistry.remove(resourceClass.getName());
    }


    @Override
    public Map<String, ActionList<? extends Resource>> getActions() {
        return Collections.unmodifiableMap(actionsRegistry);
    }
}
