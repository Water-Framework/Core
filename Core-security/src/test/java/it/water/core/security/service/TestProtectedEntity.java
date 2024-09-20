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
package it.water.core.security.service;

import it.water.core.api.permission.ProtectedEntity;
import it.water.core.permission.action.CrudActions;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.permission.annotations.DefaultRoleAccess;
import it.water.core.security.TestProtectedResource;

import java.util.Date;

@AccessControl(availableActions = {CrudActions.SAVE,"GET"},
        rolesPermissions = {
                @DefaultRoleAccess(roleName = TestProtectedResource.TEST_ROLE_NAME, actions = { CrudActions.SAVE , "GET"})
        })
public class TestProtectedEntity implements ProtectedEntity {

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.setEntityVersion(entityVersion);
    }

    @Override
    public Date getEntityCreateDate() {
        return new Date();
    }

    @Override
    public Date getEntityModifyDate() {
        return new Date();
    }

    @Override
    public Integer getEntityVersion() {
        return 0;
    }

}
