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
package it.water.core.bundle;

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.interceptors.annotations.implementation.WaterComponentsInjector;
import it.water.core.registry.model.ComponentConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @param <T>
 * @param <K>
 * @Author Aristide Cittadino.
 * This class should be used/extended in order to setup an application which uses  Framework.
 * Basically the initializeFrameworkComponents method loads all framework components defined inside the source code.
 */
public abstract class ApplicationInitializer<T, K> extends AbstractInitializer<T, K> {
    private static Logger log = LoggerFactory.getLogger(ApplicationInitializer.class);

    /**
     * Method called at application startup in order to setup framework components
     *
     * @param frameworkComponents
     */
    @Override
    protected void setupFrameworkComponents(Iterable<Class<?>> frameworkComponents) {
        frameworkComponents.iterator().forEachRemaining(component -> {
            log.debug("Found @FrameworkComponent {}", component.getName());
            FrameworkComponent frameworkComponentAnnotation = component.getAnnotation(FrameworkComponent.class);
            Optional<Constructor<?>> defaultConstructor = Arrays.stream(component.getConstructors()).filter(constructor -> constructor.getParameterCount() == 0).findAny();
            if (defaultConstructor.isEmpty()) {
                throw new UnsupportedOperationException("@FrameworkComponent " + component.getName() + " must have default constructor!");
            }
            Dictionary<String, Object> dictionary = getComponentProperties(frameworkComponentAnnotation.properties());
            List<Class<?>> services = getDeclaredServices(frameworkComponentAnnotation, component);
            log.debug("Component: {} implementing services {} with properties :\n {}", component.getName(), services, Arrays.stream(frameworkComponentAnnotation.properties()).toArray());
            try {
                Object service = defaultConstructor.get().newInstance();
                injectFields(service);
                ComponentRegistry registry = getComponentRegistry();
                services.forEach(componentClass -> {
                    try {
                        //framework component is registered with the given priority
                        ComponentConfiguration componentConfiguration = ComponentConfigurationFactory.createNewComponentPropertyFactory()
                                .withPriority(frameworkComponentAnnotation.priority())
                                .fromStringDictionary(dictionary)
                                .build();
                        ComponentRegistration<T, K> registration = (ComponentRegistration<T, K>) registry.registerComponent(componentClass, service, componentConfiguration);
                        if (registration.getComponent() != null) {
                            registeredServices.add(registration);
                            log.debug("Component: {} succesfully registered!", component.getName());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e){
                log.error("Cannot instantiate new class of {}: {}",services.getClass().getName(),e.getMessage());
            }
        });
    }

    /**
     * Method which returns a complete list of component exposed services in terms of list of interfaces put inside the @FrameworkComponent
     * or (if services property of @FrameworkComponent is empty) implemented interfaces
     *
     * @param frameworkComponentAnnotation
     * @param component
     * @return
     */
    protected List<Class<?>> getDeclaredServices(FrameworkComponent frameworkComponentAnnotation, Class<?> component) {
        List<Class<?>> services = new ArrayList<>(Arrays.asList(frameworkComponentAnnotation.services()));
        //no services specified inside annotation let's take declared interfaces
        if (services.isEmpty()) {
            Class<?>[] declaredInterfaces = component.getInterfaces();
            if (declaredInterfaces.length > 0) {
                services = new ArrayList<>(Arrays.asList(declaredInterfaces));
            }
        } else {
            checkComponentImplementsExposedServices(component, services);
        }
        return services;
    }

    /**
     * Returns component properties
     *
     * @param properties
     * @return
     */
    private Dictionary<String, Object> getComponentProperties(String[] properties) {
        Map<String, Object> dictionary = new HashMap<>();
        Arrays.stream(properties).filter(property -> property.indexOf("=") >= 0 && property.split("=").length == 2).forEach(property -> {
            String[] prop = property.split("=");
            String name = prop[0];
            String val = prop[1];
            dictionary.put(name, val);
        });
        return new Hashtable<>(dictionary);
    }

    /**
     * Check wether the method, declared as a component of specific services, implements those interfaces
     *
     * @param component
     * @param services
     */
    private void checkComponentImplementsExposedServices(Class<?> component, List<Class<?>> services) {
        services.forEach(service -> {
            if (!service.isAssignableFrom(component))
                throw new UnsupportedOperationException("@FrameworkComponent must implement exposed services interfaces!");
        });
    }

    /**
     * Utility method for inject fields needed at startup.
     * It searchs for @Inject(startup=true) fields
     *
     * @param component
     * @param <S>
     */
    private <S> void injectFields(S component) {
        log.debug("Injecting  Components into fields");
        final ComponentRegistry componentRegistry = getComponentRegistry();
        Arrays.stream(component.getClass().getDeclaredFields()).filter(field -> Arrays.stream(field.getDeclaredAnnotations())
                //filtering all Inject annotation that should be injected at startup, this is the moment
                .anyMatch(annotation -> annotation.annotationType().equals(Inject.class) && ((Inject) annotation).injectOnceAtStartup())).forEach(annotatedField -> {
            Object service = null;
            //avoiding to find component registry just injecting if some components require it
            if (annotatedField.getType().equals(ComponentRegistry.class))
                service = componentRegistry;
            else
                service = componentRegistry.findComponent(annotatedField.getType(), null);

            try {
                log.debug("Setting field {} on {}", annotatedField.getName(), component.getClass().getName());
                Method m = WaterComponentsInjector.findSetterMethod(component, annotatedField);
                m.invoke(component, service);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
        });

    }

}
