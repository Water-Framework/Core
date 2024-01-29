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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Aristide Cittadino
 * Generic action, used to wrap all user defined actions inside the system.
 */
@Data
@NoArgsConstructor
public class GenericAction implements Action {
    String actionName;
    String actionType;
    long actionId;

}
