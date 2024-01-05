
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
import it.water.core.api.action.ActionsManager;
import it.water.core.api.action.ResourceAction;
import it.water.core.api.model.Resource;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.ComponentConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @Author Aristide Cittadino.
 * Every action must be registered as a component inside the registry.
 * StandardActionsManager provide registration capabilities.
 */
public abstract class StandardActionsManager implements ActionsManager {
    protected Logger log = LoggerFactory.getLogger(StandardActionsManager.class.getName());

    private List<ComponentRegistration<?, ?>> registrations;

    @Inject
    private ComponentRegistry componentRegistry;


    protected StandardActionsManager() {
        registrations = new ArrayList<>();
    }

    /**
     * Registers a list of actions that have to be registered as OSGi components
     */
    public void registerActions() {
        log.debug("Invoking registerActions of {}", this.getClass().getSimpleName());
        Map<String, ActionList<Resource>> resourcesActions = this.getActions();
        resourcesActions.keySet().forEach(resourceName -> {
            ActionList<?> actions = resourcesActions.get(resourceName);
            actions.getList().forEach(action -> registerAction(resourceName, action));
        });
    }

    /**
     * Unregisters an action that have to be registered as OSGi components
     */
    public void unregisterActions() {
        log.debug("Invoking registerActions of {}", this.getClass().getSimpleName());
        this.registrations.parallelStream().forEach(registration -> this.componentRegistry.unregisterComponent(registration));
    }

    /**
     * Method used for dependency injection
     *
     * @param componentRegistry
     */
    public void setComponentRegistry(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    protected void registerAction(String resourceName, ResourceAction<?> action) {
        ComponentConfiguration conf = ComponentConfigurationFactory.createNewComponentPropertyFactory()
                .withProp(ActionsConstants.ACTION_RESOURCE_NAME, resourceName)
                .withProp(ActionsConstants.ACTION_NAME, action.getAction().getActionName())
                .build();
        //register action with the standard interface so it can be retrieved generically from the component registry
        ComponentRegistration<?, ?> registration = this.componentRegistry.registerComponent(ResourceAction.class, action, conf);
        registrations.add(registration);
    }

}
