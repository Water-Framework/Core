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
package it.water.core.testing.utils;

import it.water.core.api.model.ProtectedResource;
import it.water.core.permission.action.CrudActions;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.permission.annotations.DefaultRoleAccess;

@AccessControl(availableActions = {CrudActions.SAVE},
        rolesPermissions = {
                @DefaultRoleAccess(roleName = "ROLE", actions = {CrudActions.SAVE})
        })
public class TestResource implements ProtectedResource {
    @Override
    public String getResourceId() {
        return "id";
    }

    @Override
    public String getResourceName() {
        return ProtectedResource.super.getResourceName();
    }
}
