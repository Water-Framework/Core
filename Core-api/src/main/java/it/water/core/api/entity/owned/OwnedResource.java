
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

package it.water.core.api.entity.owned;

import it.water.core.api.model.BaseEntity;


/**
 * @Author Aristide Cittadino.
 * This interface tells Water system that a specific resource which implements this interface
 * should be accessed or modified by the user who have created it.
 * Plus when a new owned resource is created, the system automatically assigns to it the user id of
 * the current logged user.
 * This prevents maliciuous users to create entities with other user's ids.
 */
public interface OwnedResource extends BaseEntity {
    static final String OWNER_USER_ID_FIELD_NAME = "ownerUserId";
    /**
     * @return User who owns the entity
     */
    Long getOwnerUserId();

    void setOwnerUserId(Long userId);

    /**
     * Method returning the field name of the owner user id
     * @return
     */
    static String getOwnerUserIdFieldName(){
        return OWNER_USER_ID_FIELD_NAME;
    }
}
