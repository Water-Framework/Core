
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @Author Aristide Cittadino.
 * Class that enumerates all CRUD base actions.
 * Note that all base actions are used by entities.
 */
public enum CrudAction implements Action {
    SAVE(ActionNames.SAVE, 1),
    UPDATE(ActionNames.UPDATE, 2),
    FIND(ActionNames.FIND, 4),
    REMOVE(ActionNames.REMOVE, 8);

    @Getter
    private final String actionName;
    @Getter
    private final long actionId;

    CrudAction(String actionName, long actionId) {
        this.actionName = actionName;
        this.actionId = actionId;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ActionNames {
        public static final String SAVE = "save";
        public static final String UPDATE = "update";
        public static final String FIND = "find";
        public static final String REMOVE = "remove";
    }

    @Override
    public String getActionType() {
        return this.getClass().getName();
    }
}
