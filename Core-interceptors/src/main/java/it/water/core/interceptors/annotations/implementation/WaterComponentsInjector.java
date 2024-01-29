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

package it.water.core.interceptors.annotations.implementation;

import it.water.core.api.interceptors.BeforeMethodFieldInterceptor;
import it.water.core.api.service.Service;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


/**
 * @Author Aristide Cittadino
 * Standard implementation which injects at runtime fields annotated with @WaterInject.
 * The main differences with @Autowired or similars is that this annotations is cross-technology.
 * This means that can be used on framework components to create cross-technologies libraries.
 */
@FrameworkComponent(services = WaterComponentsInjector.class)
public class WaterComponentsInjector implements BeforeMethodFieldInterceptor<Inject> {
    private static Logger log = LoggerFactory.getLogger(WaterComponentsInjector.class);

    //injected once at application startup
    @Inject(injectOnceAtStartup = true)
    @Setter
    private ComponentRegistry componentRegistry;

    /**
     * @param destination      Water Service which is going to be invoked
     * @param m                Method
     * @param fields           fields annotated with annotation param
     * @param args             Method arguments
     * @param injectAnnotation Annotation processed on the method which maps the Interceptor definition
     * @param <S>
     */
    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, List<Field> fields, Object[] args, Inject injectAnnotation) {
        if (!injectAnnotation.injectOnceAtStartup()) {
            inject(componentRegistry, destination, fields);
        }
    }

    /**
     * @param componentRegistry
     * @param destination
     * @param fields
     * @param <S>
     */
    public static <S extends Service> void inject(ComponentRegistry componentRegistry, S destination, List<Field> fields) {
        log.debug("Injecting Water Components into fields");
        fields.forEach(annotatedField -> {
            Object service = null;
            try {
                 service = componentRegistry.findComponent(annotatedField.getType(), null);
            }catch (NoComponentRegistryFoundException e){
                log.error(e.getMessage(),e);
            }

            try {
                Method setterMethod = findSetterMethod(destination, annotatedField);
                log.debug("Setting field {} on {}", annotatedField.getName(), destination.getClass().getName());
                setterMethod.invoke(destination, service);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                log.error("Cannot inject {} field, error while invoking setter method {},", annotatedField, e.getMessage(), e);
            }
        });
    }

    /**
     * @param destination
     * @param annotatedField
     * @param <S>
     * @return
     * @throws NoSuchMethodException
     */
    public static <S> Method findSetterMethod(S destination, Field annotatedField) throws NoSuchMethodException {
        Class<?> objClass = destination.getClass();
        //annotated field must have a related setter method
        String setterMethodName = "set" + annotatedField.getName().substring(0, 1).toUpperCase() + annotatedField.getName().substring(1);
        while (objClass != null) {
            try {
                return objClass.getDeclaredMethod(setterMethodName, annotatedField.getType());
            } catch (NoSuchMethodException e) {
                log.warn("No setter method {} found on {}, trying with superclass ", setterMethodName, destination.getClass().getName());
            }
            objClass = objClass.getSuperclass();
        }
        throw new NoSuchMethodException();
    }
}
