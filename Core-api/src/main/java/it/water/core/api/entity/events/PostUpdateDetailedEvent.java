
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


/**
 * @param <T> Object representing a WaterBaseEntity
 *            <p>
 *            This interface is used for being notified for detailed update.
 *            Use this interface if you want receive the entity before the update and the entity after.
 *            Otherwise, if you are interested only in the last value, please use WaterPostUpdateAction
 * @Author Aristide Cittadino
 */
public interface PostUpdateDetailedEvent<T extends BaseEntity> extends PostCrudDetailedEvent<T> {
    /**
     * Execute update passing before update value and after update value.
     * This method invokes execute method as default.
     *
     * @param beforeCrudOperation Entity before crud action
     * @param afterCrudOperation  Entity after crud action
     */
    void execute(T beforeCrudOperation, T afterCrudOperation);
}
