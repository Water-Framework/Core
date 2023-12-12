
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
import it.water.core.api.action.ResourceAction;
import it.water.core.api.model.Resource;
import lombok.Getter;


/**
 * @Author Aristide Cittadino.
 * Class which maps the concept of action associated with specific  resource.
 */
public class DefaultResourceAction<T extends Resource> implements ResourceAction<T> {
    @Getter
    private final Action action;
    @Getter
    private final Class<T> resourceClass;

    public DefaultResourceAction(Action action, Class<T> resourceClass) {
        this.action = action;
        this.resourceClass = resourceClass;
    }
}
