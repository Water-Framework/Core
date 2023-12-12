
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

package it.water.core.api.entity.events;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.events.PostDetailedEvent;


/**
 * @param <T> Entity type
 * @Author Aristide Cittadino.
 * Interface which maps the concept of an action which is going to
 * be executed after a CRUD Actions has been run on a particular entity.
 * This kind of action is designed to give information about the entity before the crud action
 * and after.
 */
public interface PostCrudDetailedEvent<T extends BaseEntity> extends PostDetailedEvent<T> {

}
