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

package it.water.core.api.action;


import it.water.core.api.model.Resource;
import it.water.core.api.service.Service;

import java.util.Map;


/**
 * @Author Aristide Cittadino.
 * Interface abstracting the concept of actions manager.
 * An action manager is a component that takes care of registering/unregistering actions for a specific resource.
 */
public interface ActionsManager extends Service {
    /**
     * Registers actions inside the current context
     */
    <T extends Resource> void registerActions(Class<T> resourceClass);

    /**
     * Unregisters actions inside the current context
     */
    <T extends Resource> void unregisterActions(Class<T> resourceClass);

    /**
     * Key is resource class name
     * Value is actionList for each resource
     *
     * @return actions that must be registered inside the context for each resource
     */
    <T extends Resource> Map<String, ActionList<T>> getActions();
}
