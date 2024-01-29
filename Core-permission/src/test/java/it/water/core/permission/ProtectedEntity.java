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
package it.water.core.permission;


import it.water.core.permission.action.CrudActions;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.permission.annotations.DefaultRoleAccess;

import java.util.Date;

@AccessControl(
        availableActions = {CrudActions.SAVE,CrudActions.UPDATE,CrudActions.REMOVE,CrudActions.FIND},
        rolesPermissions = {
                @DefaultRoleAccess(roleName = "Role1",actions = {CrudActions.SAVE,CrudActions.UPDATE}),
                @DefaultRoleAccess(roleName = "Role2",actions = {CrudActions.FIND,CrudActions.REMOVE})
        }
)
public class ProtectedEntity implements it.water.core.api.permission.ProtectedEntity {
    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Date getEntityCreateDate() {
        return null;
    }

    @Override
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    public Integer getEntityVersion() {
        return 0;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        //do nothing
    }
    @Override
    public String getSystemApiClassName() {
        return null;
    }
}
