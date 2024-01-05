
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

package it.water.core.api.model.events;

import it.water.core.api.model.Resource;


/**
 * @Author Aristide Cittadino.
 * Interface which map the concept of an operation which can be executed after another one happened on a particular entity
 *
 * @param <T> Resource type which extends  Resource
 */
public interface PostDetailedEvent<T extends Resource> extends Event {

    /**
     * Execute an action after another one
     *
     * @param beforeEntity Entity before some operation is performed
     * @param afterEntity Entity after some operation is performed
     */
    void execute(T beforeEntity,T afterEntity);

}
