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
package it.water.core.security;

import it.water.core.api.permission.PermissionManager;
import it.water.core.security.model.context.WaterAbstractSecurityContext;

import java.security.Principal;
import java.util.Set;

public class ExampleSecurityContext extends WaterAbstractSecurityContext {
    PermissionManager permissionManager;

    public ExampleSecurityContext(PermissionManager permissionManager, Set<Principal> loggedPrincipals) {
        super(loggedPrincipals);
        this.permissionManager = permissionManager;
    }

    public ExampleSecurityContext(Set<Principal> loggedPrincipals, String permissionImplementation, PermissionManager permissionManager) {
        super(loggedPrincipals, permissionImplementation);
        this.permissionManager = permissionManager;
    }

    @Override
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return "none";
    }
}
