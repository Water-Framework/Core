/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.core.permission.action;

import it.water.core.api.action.Action;
import it.water.core.api.action.ActionList;
import it.water.core.api.action.ResourceAction;
import it.water.core.api.model.Resource;
import it.water.core.model.exceptions.WaterRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * @Author Aristide Cittadino.
 * Class which maps the concept of actions list in order to manage multiple actions
 * with a single object.
 */
public class DefaultActionList<T extends Resource> implements ActionList<T> {
    /**
     * List of actions
     */
    private List<ResourceAction<T>> actions;
    private Class<T> resourceClass;

    /**
     * Constructor of ActionList
     */
    public DefaultActionList(Class<T> resourceClass) {
        actions = new ArrayList<>();
        this.resourceClass = resourceClass;
    }


    /**
     * Add items to the list of actions
     *
     * @param action
     */
    public void addAction(Action action) {
        if (!resourceClass.isAssignableFrom(resourceClass))
            throw new WaterRuntimeException("Resource instance is not a valid type for this action list!");
        ResourceAction<T> resourceAction = ActionFactory.createResourceAction(resourceClass, action);
        this.actions.add(resourceAction);
    }

    /**
     * Gets an actions list
     *
     * @return actions list
     */
    public List<ResourceAction<T>> getList() {
        //Sorting based on actionIds
        Comparator<ResourceAction<T>> comparator = (o1, o2) -> {
            if (o1.getAction().getActionId() > o2.getAction().getActionId())
                return 1;
            else if (o1.getAction().getActionId() < o2.getAction().getActionId())
                return -1;

            return 0;
        };
        Collections.sort(this.actions, comparator);
        return this.actions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            sb.append("Action " + actions.get(i).getAction().getActionName() + " - " + actions.get(i).getAction().getActionId() + "\n");
        }
        return sb.toString();
    }

}
