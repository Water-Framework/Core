
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

package it.water.core.security.annotations.implementation;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.permission.PermissionUtil;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.annotations.AllowRoles;
import it.water.core.permission.exceptions.UnauthorizedException;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * This class is the implementation of @AllowRoles annotation.
 * It simply verifies that the user has the a specific role before execute the invoked method.
 */
@FrameworkComponent(services = {BeforeMethodInterceptor.class})
public class AllowRolesInterceptor extends AbstractPermissionInterceptor implements BeforeMethodInterceptor<AllowRoles> {
    private static Logger log = LoggerFactory.getLogger(AllowRolesInterceptor.class.getName());
    @Inject
    @Setter
    private PermissionUtil waterPermissionUtil;

    @Inject
    @Setter
    private Runtime waterRuntime;

    /**
     * @param s          Water Service which is going to be invoked
     * @param m          Method
     * @param args       Method arguments
     * @param annotation Annotation processed on the method which maps the Interceptor definition
     */
    @Override
    public void interceptMethod(Service s, Method m, Object[] args, AllowRoles annotation) {
        log.debug("Invoking interceptor @AllowRoles on method: {}", m.getName());
        this.checkAnnotationIsOnWaterApiClass(s, annotation);
        if (annotation.rolesNames() == null || annotation.rolesNames().length == 0)
            throw new WaterRuntimeException("@AllowRoles needs at least one role name");
        String[] roles = annotation.rolesNames();
        SecurityContext ctx = waterRuntime.getSecurityContext();
        if (!waterPermissionUtil.userHasRoles(ctx.getLoggedUsername(), roles))
            throw new UnauthorizedException();
    }

    /**
     * @return
     */
    @Override
    public Class getAnnotation() {
        return AllowRoles.class;
    }
}
