
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

package it.water.core.api.permission;

import it.water.core.api.service.Service;

/**
 * @Author Aristide Cittadino.
 * Generic interface component for SecurityContext.
 * This interface defines methods to provide security information of
 * user during interactions with the  platform.
 * This interfaces extends both cxf SecurityContext and jaxrs Security Context
 */
public interface SecurityContext extends Service {
    /**
     * Returns a string indicating the name of the authenticated current user.
     */
    String getLoggedUsername();

    /**
     * Returns a boolean value indicating that the user has logged in.
     */
    boolean isLoggedIn();

    /**
     * True if the logged user is admin
     *
     * @return
     */
    boolean isAdmin();

    /**
     * @return the id of the logged entity
     */
    long getLoggedEntityId();

    /**
     * @return the permission manager from the security context
     */
    PermissionManager getPermissionManager();

}
