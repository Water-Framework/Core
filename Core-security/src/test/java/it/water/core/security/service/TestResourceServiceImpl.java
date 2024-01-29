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

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.permission.annotations.AllowGenericPermissions;
import it.water.core.permission.annotations.AllowRoles;

@FrameworkComponent(services = {TestResourceService.class})
public class TestResourceServiceImpl implements TestResourceService {

    @AllowGenericPermissions(actions = "GET", resourceName = "it.water.core.security.TestProtectedResource")
    public boolean genericPermissionMethod() {
        return true;
    }

    @AllowGenericPermissions(actions = "GET", resourceParamName = "name")
    public boolean genericPermissionMethodWithResourceParamName(String name) {
        return true;
    }

    @AllowRoles(rolesNames = "ROLE")
    public boolean allowRolesMethod() {
        return true;
    }
}
