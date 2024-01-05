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
package it.water.core.testing.utils.security;

import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.SecurityContext;

public class TestSecurityContext implements SecurityContext {
    private String loggedUserName;
    private boolean isAdmin;
    private long id;
    private PermissionManager permissionManager;

    private TestSecurityContext(String loggedUserName, boolean isAdmin, long id, PermissionManager permissionManager) {
        this.loggedUserName = loggedUserName;
        this.isAdmin = isAdmin;
        this.id = id;
        this.permissionManager = permissionManager;
    }

    @Override
    public String getLoggedUsername() {
        return loggedUserName;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedUserName != null;
    }

    @Override
    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public long getLoggedEntityId() {
        return id;
    }

    @Override
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public static final TestSecurityContext createContext(long id, String username, boolean isAdmin, PermissionManager permissionManager) {
        return new TestSecurityContext(username, isAdmin, id, permissionManager);
    }

}
