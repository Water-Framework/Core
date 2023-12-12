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
package it.water.core.permission.action.model;

import it.water.core.permission.action.DefaultActionList;
import it.water.core.permission.action.StandardActionsManager;
import it.water.core.permission.action.WebAPIAction;

import java.util.HashMap;
import java.util.Map;

public class TestActionsManager extends StandardActionsManager {

    @Override
    public Map<String, DefaultActionList<TestResource>> getActions() {
        Map<String, DefaultActionList<TestResource>> actions = new HashMap<>();
        DefaultActionList<TestResource> actionList = new DefaultActionList<>(TestResource.class);
        actionList.addAction(WebAPIAction.GET);
        actions.put(TestResource.class.getName(), actionList);
        return actions;
    }

    @Override
    public void registerActions() {
        //do nothing for test
    }

    @Override
    public void unregisterActions() {
        //do nothing for test
    }
}
