
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

/**
 * @Author Aristide Cittadino.
 * This interface maps the concept of action in terms of operations that users can do on some resources.
 * Every action must have an actionId which is a power of 2.
 * Actions are the basic bricks to perform permission checking in a high granular way.
 */
public interface Action {
    /**
     *
     * @return name of the action ie. SAVE, UPDATE
     */
    String getActionName();

    /**
     *
     * @return type of the action ie. it.water.core.action.CrudAction
     */
    String getActionType();

    /**
     *
     * @return Pow of 2, representing the action id inside the type
     */
    long getActionId();
}
