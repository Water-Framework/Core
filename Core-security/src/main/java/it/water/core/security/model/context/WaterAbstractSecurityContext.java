
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

package it.water.core.security.model.context;

import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.security.model.SecurityConstants;
import it.water.core.security.model.principal.RolePrincipal;
import it.water.core.security.model.principal.UserPrincipal;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @Author Aristide Cittadino
 * Abstract Security Context to generalize security management.
 */
public abstract class WaterAbstractSecurityContext implements SecurityContext {

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;
    private Set<java.security.Principal> loggedPrincipals;
    private java.security.Principal loggedUser;
    private List<RolePrincipal> roles;
    /**
     * indicates which permissions implementation to use
     */
    @Getter
    private String permissionImplementation;

    /**
     * Since User and Devices can login inside the platform, it should necessary to identify which kind of entity is logged in
     */
    @Getter
    private String issuerClassName;

    /**
     * Logged User Id
     */
    @Getter
    private long loggedEntityId;

    protected WaterAbstractSecurityContext(Set<java.security.Principal> loggedPrincipals) {
        this.setLoggedPrincipals(loggedPrincipals);
        this.permissionImplementation = SecurityConstants.PERMISSION_MANAGER_DEFAULT_IMPLEMENTATION;
    }

    protected WaterAbstractSecurityContext(Set<java.security.Principal> loggedPrincipals, String permissionImplementation) {
        this.setLoggedPrincipals(loggedPrincipals);
        this.permissionImplementation = permissionImplementation;
    }

    /**
     * Defines all logged principals.
     * There are 2 types of Principals WaterPrincipal which extends Principal and WaterRolePrincipal
     *
     * @param loggedPrincipals
     */
    public void setLoggedPrincipals(Set<java.security.Principal> loggedPrincipals) {
        this.loggedPrincipals = loggedPrincipals;
        if (loggedPrincipals != null) {
            Iterator<java.security.Principal> it = loggedPrincipals.iterator();
            roles = new ArrayList<>();
            while (it.hasNext()) {
                java.security.Principal p = it.next();
                if (p instanceof UserPrincipal) {
                    this.loggedUser = p;
                    this.loggedEntityId = ((UserPrincipal) p).getLoggedEntityId();
                    this.issuerClassName = ((UserPrincipal) p).getIssuer();
                }

                if (p instanceof RolePrincipal) {
                    roles.add((RolePrincipal) p);
                }
            }
        } else {
            this.loggedPrincipals = new HashSet<>();
        }
    }

    /**
     * Returns a string indicating the name of the authenticated current user. If
     * the user has not been authenticated, the method returns null.
     */
    @Override
    public String getLoggedUsername() {
        return (loggedUser != null) ? loggedUser.getName() : null;
    }

    /**
     * Returns a boolean value indicating that the user has logged in.
     */
    @Override
    public boolean isLoggedIn() {
        return loggedUser != null && this.loggedUser.getName().length() > 0;
    }

    /**
     * If the logged principal is admin
     *
     * @return
     */
    @Override
    public boolean isAdmin() {
        if (loggedUser == null) return false;
        UserPrincipal p = (UserPrincipal) loggedUser;
        return p.isAdmin();
    }


    /**
     * Return information about user, if it is a user logged or not yet.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Water Context:");
        if (this.getLoggedUsername() != null) sb.append("Logged User:" + this.getLoggedUsername());
        else sb.append("Guest user");
        return sb.toString();
    }

    /**
     * @return
     */
    public java.security.Principal getUserPrincipal() {
        return this.loggedUser;
    }

    /**
     * Checks wether current user has a specific role
     *
     * @param role
     * @return
     */
    public boolean isUserInRole(String role) {
        if (this.roles == null || this.roles.isEmpty()) return false;

        for (int i = 0; i < this.roles.size(); i++) {
            if (this.roles.get(i).getName().equalsIgnoreCase(role)) return true;
        }
        return false;
    }

    /**
     * @return Set of logged principals
     */
    public Set<java.security.Principal> getLoggedPrincipals() {
        return Collections.unmodifiableSet(this.loggedPrincipals);
    }

    /**
     * @return Permission Manager based on the value of the permissionImplementation String
     */
    public PermissionManager getPermissionManager() {
        this.permissionImplementation = this.permissionImplementation == null ? SecurityConstants.PERMISSION_MANAGER_DEFAULT_IMPLEMENTATION : this.permissionImplementation;
        ComponentFilterBuilder componentFilterBuilder = componentRegistry.getComponentFilterBuilder();
        ComponentFilter permissionManagerFilter = componentFilterBuilder.createFilter(SecurityConstants.PERMISSION_IMPLEMENTATION_COMPONENT_PROPERTY, this.permissionImplementation);
        return componentRegistry.findComponent(PermissionManager.class, permissionManagerFilter);
    }

    public abstract boolean isSecure();

    public abstract String getAuthenticationScheme();
}
