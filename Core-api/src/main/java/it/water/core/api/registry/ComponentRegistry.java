
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

package it.water.core.api.registry;

import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.service.BaseEntitySystemApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @Author Aristide Cittadino.
 * This interface represents the concept of beans registry, where beans are registered or retrieved.
 * There'll be N implementation of this interface for Spring, Quarkus , OSGi and other frameworks...
 */
public interface ComponentRegistry {
    Logger log = LoggerFactory.getLogger(ComponentRegistry.class);

    /**
     * Finds registered components in priority order
     *
     * @param componentClass Component interface
     * @param filter         filter to identify the specific required component
     * @param <T>            component interface
     * @return list of component found inside the registry
     */
    <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter);

    /**
     * Return the highest priority component found (framework components have a priority of 1 which means lowest priority)
     *
     * @param componentClass Component interface
     * @param filter         filter to identify the specific required component
     * @param <T>            component interface
     * @return Single component, if found many it returns the first
     */
    <T> T findComponent(Class<T> componentClass, ComponentFilter filter);

    /**
     * @param componentClass service class
     * @param component      component which must be registered
     * @param configuration  configuration associated with this component, can be null
     * @param <T>
     * @return the registration object with some details about the bean and some other info specific of the framework
     */
    <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration);

    /**
     * @param registration the component registration
     * @return true if component has been unregistered
     */
    <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration);

    /**
     * @param component the component that has to be unregistered
     * @return true if component has been unregistered
     */
    <T> boolean unregisterComponent(Class<T> componentClass, T component);

    /**
     * @return the object deputed to build filter for retrieving components
     */
    ComponentFilterBuilder getComponentFilterBuilder();

    /**
     * Returns the system api associated with a specific entity class.
     * Useful in context where you want to manage an entity starting from the class name in a generic way.
     * @param entityClassName
     * @return
     * @param <T>
     */
    <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName);

    /**
     * Method used to invoke lifecycle methods like @OnActivate or @OnDeactivate
     *
     * @param annotation annotation which must be found on the method in order to be invoked
     * @param component  on which the method must be executed
     * @param <T>
     */
    default <T> void invokeLifecycleMethod(Class<? extends Annotation> annotation, Class<?> componentServiceClass, T component) {
        log.debug("Running activation method on component: {}", componentServiceClass.getName());
        Collection<Method> methods = Arrays.stream(componentServiceClass.getMethods()).filter(method -> Arrays.stream(method.getDeclaredAnnotations()).anyMatch(methodAnnotation -> methodAnnotation.annotationType().equals(annotation))).collect(Collectors.toSet());
        methods.forEach(method -> {
            try {
                //injecting parameters from component registry
                Class<?>[] parameters = method.getParameterTypes();
                Object[] parameterValues = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    Object parameter = null;
                    try {
                        parameter = this.findComponent(parameters[i], null);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                    parameterValues[i] = parameter;
                }

                if (Proxy.isProxyClass(component.getClass())) {
                    //proxies do not expose annotations so, basically we search for the same method inside the proxied instance
                    //todo now activate method on water services must be exposed in the service interface. it should be possible to invoke activation method even if it's not exposed on the interface allowing always fields injection
                    //todo check not only parameters count but parameters types and order
                    Optional<Method> proxiedMethodOpt = Arrays.stream(component.getClass().getDeclaredMethods()).filter(curMethod -> curMethod.getName().equals(method.getName()) && curMethod.getReturnType().equals(method.getReturnType()) && curMethod.getParameterCount() == method.getParameterCount()).findFirst();
                    if (proxiedMethodOpt.isPresent())
                        proxiedMethodOpt.get().invoke(component, parameterValues);
                } else {
                    method.invoke(component, parameterValues);
                }
            } catch (Exception e) {
                log.error("Error while executing {} lifecycle method {}", annotation.getClass().getName(), method.getName());
                log.error(e.getMessage(), e);
            }
        });
    }
}
