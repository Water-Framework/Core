
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

import it.water.core.api.action.Action;
import it.water.core.api.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino.
 * Factory class to create actions associated with resources.
 */
public class ActionFactory {
    protected static Logger log = LoggerFactory.getLogger(ActionFactory.class.getName());

    private ActionFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param resourceClass water resource
     * @param action        water action
     * @return creates new resource action
     */
    public static <T extends Resource> DefaultResourceAction<T> createResourceAction(Class<T> resourceClass, Action action) {
        log.debug("Invoking createAction for resource {} with action name {}", resourceClass.getName(), action.getActionName());
        return new DefaultResourceAction<>(action, resourceClass);
    }

    /**
     * @param resourceClass the resource assocaited with actionList
     * @return a list actions that have to be registered
     */
    public static <T extends Resource> DefaultActionList<T> createBaseCrudActionList(Class<T> resourceClass) {
        log.debug("Invoking createBaseCrudActionList for resource {}", resourceClass);
        DefaultActionList<T> actionList = new DefaultActionList<>(resourceClass);
        actionList.addAction(CrudAction.SAVE);
        actionList.addAction(CrudAction.UPDATE);
        actionList.addAction(CrudAction.REMOVE);
        actionList.addAction(CrudAction.FIND);
        return actionList;
    }

    /**
     * Initialize an empty list of actions
     */
    public static <T extends Resource> DefaultActionList<T> createEmptyActionList(Class<T> resourceClass) {
        return new DefaultActionList<>(resourceClass);
    }
}
