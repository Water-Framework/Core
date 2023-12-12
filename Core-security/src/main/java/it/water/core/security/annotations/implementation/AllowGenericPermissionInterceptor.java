
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

package it.water.core.security.annotations.implementation;


import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.permission.PermissionUtil;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.BaseEntityApi;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.annotations.AllowGenericPermissions;
import it.water.core.permission.exceptions.UnauthorizedException;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * This class is the implementation of @AllowGenericPermission annotation.
 * It simply verifies that the user has the permission to do some action on the resource name (without id).
 * It's a generic permission, this means that permission verification is based only on the resource name and action name not on the entity id
 * <p>
 * The method tries to automatically infers the entity type (in case of EntityApi).
 */
@FrameworkComponent(services = {BeforeMethodInterceptor.class})
public class AllowGenericPermissionInterceptor extends AbstractPermissionInterceptor implements BeforeMethodInterceptor<AllowGenericPermissions> {
    private static Logger log = LoggerFactory.getLogger(AllowGenericPermissionInterceptor.class.getName());

    @Inject
    @Setter
    private PermissionUtil permissionUtil;

    /**
     * @param s          Water Service which is going to be invoked
     * @param m          Method
     * @param args       Method arguments
     * @param annotation Annotation processed on the method which maps the Interceptor definition
     */
    @Override
    public void interceptMethod(Service s, Method m, Object[] args, AllowGenericPermissions annotation) {
        log.debug("Invoking interceptor @AllowPermission on method: {}", m.getName());
        this.checkAnnotationIsOnWaterApiClass(s, annotation);
        String[] actions = annotation.actions();
        String resourceName = null;
        SecurityContext ctx = getWaterRuntime().getSecurityContext();
        if (s instanceof BaseEntityApi) {
            resourceName = getResourceNameFromEntity(s, args, annotation);
        } else {
            resourceName = getResourceNameFromGenericResource(args, annotation);
        }

        if (ctx != null) {
            boolean found = false;
            for (int i = 0; !found && i < actions.length; i++) {
                found = permissionUtil.checkPermission(resourceName,
                        this.getAction(resourceName, actions[i]));
            }
            if (!found)
                throw new UnauthorizedException();
            return;
        }
        throw new UnauthorizedException();
    }

    /**
     * @param s
     * @param args
     * @param annotation
     * @return
     */
    private String getResourceNameFromEntity(Service s, Object[] args, AllowGenericPermissions annotation) {
        String resourceName = null;
        //we must check entity permissions
        BaseEntityApi<?> entityApi = (BaseEntityApi<?>) s;
        //User can customize resource name on which permissions must be checked
        if (annotation.resourceName().length() > 0) {
            resourceName = annotation.resourceName();
        } else if (annotation.resourceParamName().length() > 0) {
            int index = findMethodParamIndexByName(annotation.resourceParamName(), args);
            if (index > 0) {
                resourceName = (String) args[index];
            } else {
                throw new WaterRuntimeException("Resource Name Param " + annotation.resourceParamName() + " Not exists!");
            }
        } else
            resourceName = entityApi.getEntityType().getName();
        return resourceName;
    }

    /**
     * @param args
     * @param annotation
     * @return
     */
    private String getResourceNameFromGenericResource(Object[] args, AllowGenericPermissions annotation) {
        String resourceName = null;
        if (annotation.resourceName().length() == 0 && annotation.resourceParamName().length() == 0) {
            throw new WaterRuntimeException("@AllowGenericPermission needs a resource name! with resourceName param or resourceParamName!");
        }
        if (annotation.resourceName().length() > 0) {
            resourceName = annotation.resourceName();
        } else {
            int index = findMethodParamIndexByName(annotation.resourceParamName(), args);
            if (index >= 0) {
                resourceName = (String) args[index];
            } else {
                throw new WaterRuntimeException("Resource Name Param " + annotation.resourceParamName() + " Not exists!");
            }
        }
        return resourceName;
    }
}
