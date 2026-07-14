
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

package it.water.core.security.model.principal;

import lombok.Getter;

import java.io.Serializable;


@Getter
public class UserPrincipal implements java.security.Principal, Serializable {
    /**
     *
     */
    private String name;
    private boolean isAdmin;
    private long loggedEntityId;
    private String issuer;
    private Long companyId;
    private String impersonatedBy;

    public UserPrincipal(String name, boolean isAdmin, long loggedEntityId, String issuer) {
        this(name, isAdmin, loggedEntityId, issuer, null);
    }

    public UserPrincipal(String name, boolean isAdmin, long loggedEntityId, String issuer, Long companyId) {
        this(name, isAdmin, loggedEntityId, issuer, companyId, null);
    }

    public UserPrincipal(String name, boolean isAdmin, long loggedEntityId, String issuer, Long companyId, String impersonatedBy) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.loggedEntityId = loggedEntityId;
        this.issuer = issuer;
        this.companyId = companyId;
        this.impersonatedBy = impersonatedBy;
    }
}
