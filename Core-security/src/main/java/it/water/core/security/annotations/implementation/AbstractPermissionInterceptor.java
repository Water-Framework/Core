
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

import it.water.core.api.action.Action;
import it.water.core.api.action.ResourceAction;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.PermissionUtil;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.service.BaseApi;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.action.ActionsConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author Aristide Cittadino
 * Abstract logic for interceptors
 */
public abstract class AbstractPermissionInterceptor implements Service {
    private Logger log = LoggerFactory.getLogger(AbstractPermissionInterceptor.class.getName());

    //if found inside @ComponentFramework this field is automatically injected
    @Inject
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private Runtime waterRuntime;

    @Inject
    @Setter
    private PermissionUtil waterPermissionUtil;

    @Inject
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private ComponentRegistry componentRegistry;

    /**
     * Return current class name and action name registered as OSGi components
     *
     * @param className  parameter that indicates the class name
     * @param actionName parameter that indicates the action name
     * @return class name and action name registered as OSGi components
     */
    protected Action getAction(String className, String actionName) {
        log.debug(
                "Service getAction for {} and action {}", className, actionName);
        ComponentFilter componentFilter = componentRegistry.getComponentFilterBuilder()
                .createFilter(ActionsConstants.ACTION_RESOURCE_NAME, className)
                .and(ActionsConstants.ACTION_NAME, actionName);
        log.debug(
                "Searching for OSGi registered action with filter: {}", componentFilter.getFilter());
        List<ResourceAction<?>> actions = getActions(componentFilter);

        if (actions.size() > 1) {
            log.error("More OSGi action found for filter: {}", componentFilter.getFilter());
            throw new WaterRuntimeException();
        } else if (actions.isEmpty()) {
            return null;
        }
        ResourceAction<?> act = actions.iterator().next();
        log.debug("OSGi action found {}", act);
        return act.getAction();
    }

    private List<ResourceAction<?>> getActions(ComponentFilter componentFilter) {
        List<ResourceAction<?>> actions = new ArrayList<>();
        try {
            //Just to avoid sonar error, all objects are WaterResourceAction<?>
            List<?> foundActions = componentRegistry.findComponents(ResourceAction.class, componentFilter);
            for (Object action : foundActions) {
                actions.add((ResourceAction<?>) action);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return actions;
    }


    /**
     * Find first param with specific type inside a method
     *
     * @param type
     * @param args
     * @param <T>
     * @return
     */
    protected <T extends Resource> T findObjectTypeInParams(Class<T> type, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (type.isAssignableFrom(args[i].getClass())) {
                return (T) args[i];
            }
        }
        return null;
    }

    /**
     * @param paramName
     * @param parameters
     * @return
     */
    protected int findMethodParamIndexByName(String paramName, Object[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            if (param.equals(paramName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param ctx
     * @param entity
     * @param actions
     * @return
     */
    protected boolean checkEntityPermission(SecurityContext ctx, BaseEntity entity, String[] actions) {
        boolean found = false;
        if (ctx != null && entity != null) {
            for (int i = 0; !found && i < actions.length; i++) {
                found = waterPermissionUtil.checkPermission(entity,
                        this.getAction(entity.getResourceName(), actions[i]));
            }
        }
        return found;
    }

    /**
     * @param s
     * @param a
     */
    protected void checkAnnotationIsOnWaterApiClass(Service s, Annotation a) {
        if (!(s instanceof BaseApi))
            throw new WaterRuntimeException("Annotation " + a.annotationType().getName() + " Is accepted only on Api Classes,not on System or other services!");
    }
}
