
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

import it.water.core.api.interceptors.AfterMethodInterceptor;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.BaseEntityApi;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.annotations.AllowPermissionsOnReturn;
import it.water.core.permission.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * This class is the implementation of @AllowPermissionOnReturn annotation.
 * It simply verifies that the user has the permission to do some action on the specified entity returned by a method.
 * @see AllowPermissionInterceptor
 */
@FrameworkComponent(services = {AfterMethodInterceptor.class})
public class AllowPermissionOnReturnInterceptor extends AbstractPermissionInterceptor implements AfterMethodInterceptor<AllowPermissionsOnReturn> {
    private static Logger log = LoggerFactory.getLogger(AllowPermissionOnReturnInterceptor.class.getName());

    /**
     * @param s            Water Service which is going to be invoked
     * @param m            Method
     * @param args         Method arguments
     * @param returnResult Object returned by the method
     * @param annotation   Annotation processed on the method which maps the Interceptor definition
     */
    @Override
    public void interceptMethod(Service s, Method m, Object[] args, Object returnResult, AllowPermissionsOnReturn annotation) {
        log.debug("Invoking interceptor @AllowPermission on method: {}", m.getName());
        this.checkAnnotationIsOnWaterApiClass(s, annotation);
        //if result is null we simply go on
        if(returnResult == null)
            return;
        String[] actions = annotation.actions();
        if (s instanceof BaseEntityApi) {
            //we must check entity permissions
            SecurityContext ctx = getWaterRuntime().getSecurityContext();
            //TODO: we assume that an entity api with this annotation returns a result of entity, no collections. We should provide it
            if (returnResult instanceof BaseEntity entity) {
                boolean found = this.checkEntityPermission(ctx, entity, actions);
                if (!found)
                    throw new UnauthorizedException();
                return;
            }
            throw new WaterRuntimeException(annotation.annotationType().getName() + " is incompatible with the return type of method " + m.getName());
        } else {
            //do nothing
        }
        throw new UnauthorizedException();
    }

    @Override
    public Class getAnnotation() {
        return AllowPermissionsOnReturn.class;
    }
}
