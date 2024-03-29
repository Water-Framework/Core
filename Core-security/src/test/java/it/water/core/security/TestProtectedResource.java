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
package it.water.core.security;

import it.water.core.api.model.ProtectedResource;
import it.water.core.permission.action.CrudActions;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.permission.annotations.DefaultRoleAccess;


@AccessControl(availableActions = {CrudActions.SAVE,"GET"},
        rolesPermissions = {
                @DefaultRoleAccess(roleName = TestProtectedResource.TEST_ROLE_NAME, actions = { CrudActions.SAVE , "GET"})
        })
public class TestProtectedResource implements ProtectedResource {
    public static final String TEST_ROLE_NAME = "TestRole";
    @Override
    public String getResourceId() {
        return this.getResourceName();
    }

    @Override
    public String getResourceName() {
        return ProtectedResource.super.getResourceName();
    }
}
