
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


import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.BaseEntityApi;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.annotations.AllowPermissions;
import it.water.core.permission.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * This class is the implementation of @AllowPermission annotation.
 * It simply verifies that the user has the permission to do some action on the specified entity.
 * It's a specific permission, this means that permission verification is based  on the resource name and action name but eventually on the specific permission
 * on the specific entity.
 * <p>
 * The method tries to automatically infers the entity type (in case of EntityApi).
 */
@FrameworkComponent(services = BeforeMethodInterceptor.class)
public class AllowPermissionInterceptor extends AbstractPermissionInterceptor implements BeforeMethodInterceptor<AllowPermissions> {
    private static Logger log = LoggerFactory.getLogger(AllowPermissionInterceptor.class.getName());

    /**
     * @param s          Water Service which is going to be invoked
     * @param m          Method
     * @param args       Method arguments
     * @param annotation Annotation processed on the method which maps the Interceptor definition
     */
    @Override
    public void interceptMethod(Service s, Method m, Object[] args, AllowPermissions annotation) {
        log.debug("Invoking interceptor @AllowPermission on method: {}", m.getName());
        this.checkAnnotationIsOnWaterApiClass(s, annotation);
        String[] actions = annotation.actions();
        if (serviceIsRelatedToEntityService(s, annotation)) {
            //we must check entity permissions
            SecurityContext ctx = getWaterRuntime().getSecurityContext();
            BaseEntity entity = null;
            if (annotation.checkById()) {
                entity = this.findEntity(s, m, args, annotation);
            } else {
                entity = this.findObjectTypeInParams(BaseEntity.class, args);
            }
            boolean found = this.checkEntityPermission(ctx, entity, actions);
            if (!found)
                throw new UnauthorizedException();

        } else {
            throw new UnsupportedOperationException("@AllowPermission is not allowed on Generic Api, please us @AllowGenericPermission instead");
        }
    }

    /**
     * Checks wether annotation is put on a method relate to an entity service or use a system entity api
     *
     * @param s
     * @param annotation
     * @return
     */
    private boolean serviceIsRelatedToEntityService(Service s, AllowPermissions annotation) {
        if (s instanceof BaseEntityApi)
            return true;
        if (annotation.systemApiRef().isEmpty())
            return false;
        BaseEntitySystemApi<?> entitySystemApi = this.findSystemApi(annotation.systemApiRef());
        return entitySystemApi != null;
    }

    /**
     * returns the related system api
     *
     * @param systemApiRef
     * @return
     */
    private BaseEntitySystemApi<?> findSystemApi(String systemApiRef) {
        try {
            return (BaseEntitySystemApi) getComponentRegistry().findComponent(Class.forName(systemApiRef), null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param s
     * @param m
     * @param args
     * @param annotation
     * @return
     */
    private BaseEntity findEntity(Service s, Method m, Object[] args, AllowPermissions annotation) {
        BaseEntity entity = null;
        int entityIdIndex = annotation.idParamIndex();
        if (entityIdIndex >= 0) {
            if (!(args[entityIdIndex] instanceof Long))
                throw new WaterRuntimeException("Parameter with id: " + annotation.idParamIndex() + " on method " + m.getName() + " is not a valid id type, must be long!");
            //using id finder
            if (annotation.systemApiRef().isEmpty()) {
                entity = ((BaseEntityApi<?>) s).find((long) args[entityIdIndex]);
            } else {
                //if systemApiRef name is specified , retrieve the entity from the related system api
                //it must be entity System api for sure since there's an id related to it
                BaseEntitySystemApi<?> entitySystemApi = this.findSystemApi(annotation.systemApiRef());
                if (entitySystemApi != null) {
                    entity = entitySystemApi.find((long) args[entityIdIndex]);
                } else {
                    throw new WaterRuntimeException("Entity System Api specified (" + annotation.systemApiRef() + ") not found!");
                }
            }
        } else {
            throw new WaterRuntimeException("Impossible to find parameter with name : " + annotation.idParamIndex());
        }
        return entity;
    }

}
