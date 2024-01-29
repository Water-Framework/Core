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

import java.util.List;

/**
 * @Author Aristide Cittadino
 * @param <T> The resource related to this action list
 */
public interface ActionList<T extends Resource>{
    /**
     *
     * @param action action to add to the current resource action list
     */
    void addAction(Action action);

    /**
     *
     * @param actionName
     * @return true if the current action list contains an action with the given actionName
     */
    boolean containsActionName(String actionName);

    /**
     *
     * @param actionName
     * @return the specific action related to the action name
     */
    Action getAction(String actionName);

    /**
     *
     * @return Read-only copy of the action list
     */
    List<ResourceAction<T>> getList();
}
