
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
import lombok.Getter;


/**
 * @Author Aristide Cittadino.
 * Class that enumerates all Web API base actions.
 * Note that all base actions are used by entities in the  system.
 */
public enum WebAPIAction implements Action {
    GET(ActionNames.GET, 1),
    POST(ActionNames.POST, 2),
    PUT(ActionNames.PUT, 4),
    DELETE(ActionNames.DELETE, 8),
    HEAD(ActionNames.HEAD, 16),
    OPTIONS(ActionNames.OPTIONS, 32);

    @Getter
    private final String actionName;
    @Getter
    private final long actionId;

    WebAPIAction(String actionName, int actionId) {
        this.actionName = actionName;
        this.actionId = actionId;
    }

    public static class ActionNames {
        private ActionNames() {
        }

        public static final String GET = "get";
        public static final String POST = "post";
        public static final String PUT = "put";
        public static final String DELETE = "delete";
        public static final String HEAD = "head";
        public static final String OPTIONS = "options";
    }

    @Override
    public String getActionType() {
        return this.getClass().getName();
    }

}
