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
import it.water.core.api.service.Service;
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
        Map<Class<?>, Integer> componentsPriorities = loadComponentPriorities(frameworkComponents);
        frameworkComponents.iterator().forEachRemaining(component -> {
            log.debug("Found @FrameworkComponent {}", component.getName());
            FrameworkComponent frameworkComponentAnnotation = component.getAnnotation(FrameworkComponent.class);
            //if current component priority is listed in the component priority list
            //this means this component has the highest priority so it's primary
            boolean isPrimary = checkComponentIsPrimary(componentsPriorities, frameworkComponentAnnotation);
            Optional<Constructor<?>> defaultConstructor = Arrays.stream(component.getConstructors()).filter(constructor -> constructor.getParameterCount() == 0).findAny();
            if (defaultConstructor.isEmpty()) {
                throw new UnsupportedOperationException("@FrameworkComponent " + component.getName() + " must have default constructor!");
            }
            Dictionary<String, Object> dictionary = getComponentProperties(frameworkComponentAnnotation.properties());
            try {
                Object service = defaultConstructor.get().newInstance();
                injectFields(service);
                ComponentRegistry registry = getComponentRegistry();
                List<Class<?>> services = null;
                if (registerMultiInterfaceComponents()) {
                    //register one component for each implemented interface
                    services = getDeclaredServices(frameworkComponentAnnotation, component, service);
                } else {
                    //register just the component with its class because the registry will automatically
                    //discover all implemented interfaces. It depends on technology: OSGi works different from spring and quarkus
                    services = Collections.singletonList(service.getClass());
                }
                log.debug("Component: {} implementing services {} with properties :\n {}", component.getName(), services, Arrays.stream(frameworkComponentAnnotation.properties()).toArray());
                registerComponent(services, service, frameworkComponentAnnotation, isPrimary, registry, dictionary);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                log.error("Cannot instantiate new class of {}: {}", component.getName(), e.getMessage());
            }
        });
    }

    protected boolean registerMultiInterfaceComponents() {
        return true;
    }

    private Map<Class<?>, Integer> loadComponentPriorities(Iterable<Class<?>> frameworkComponents) {
        Map<Class<?>, Integer> componentsPriorities = new HashMap<>();
        frameworkComponents.forEach(component -> {
            FrameworkComponent frameworkComponentAnnotation = component.getAnnotation(FrameworkComponent.class);
            int componentPriority = frameworkComponentAnnotation.priority();
            List<Class<?>> services = getDeclaredServices(frameworkComponentAnnotation, component, null);
            services.forEach(service -> {
                componentsPriorities.computeIfAbsent(service, keyComponent -> componentPriority);
                if (componentsPriorities.containsKey(service) && componentsPriorities.get(service) < componentPriority)
                    componentsPriorities.put(service, frameworkComponentAnnotation.priority());
            });
        });
        return componentsPriorities;
    }

    private boolean checkComponentIsPrimary(Map<Class<?>, Integer> componentsPriorities, FrameworkComponent frameworkComponentAnnotation) {
        int priority = frameworkComponentAnnotation.priority();
        return Arrays.stream(frameworkComponentAnnotation.services()).filter(service -> componentsPriorities.get(service) == priority).findAny().isPresent();
    }

    /**
     * Method which returns a complete list of component exposed services in terms of list of interfaces put inside the @FrameworkComponent
     * or (if services property of @FrameworkComponent is empty) implemented interfaces
     *
     * @param frameworkComponentAnnotation
     * @param component
     * @return
     */
    protected List<Class<?>> getDeclaredServices(FrameworkComponent frameworkComponentAnnotation, Class<?> component, Object instance) {
        List<Class<?>> services = new ArrayList<>(Arrays.asList(frameworkComponentAnnotation.services()));
        boolean isWaterService = instance != null && Service.class.isAssignableFrom(instance.getClass());
        //no services specified inside annotation let's take declared interfaces
        if (services.isEmpty()) {
            Class<?>[] declaredInterfaces = component.getInterfaces();
            if (declaredInterfaces.length > 0) {
                services = new ArrayList<>(Arrays.asList(declaredInterfaces));
            }
        } else {
            checkComponentImplementsExposedServices(component, services);
        }
        //if the current component is a water service, but water service is not listed inside interfaces list
        //the only way is that the concrete class has a superclass which is a water Service so we add explicitily
        if (isWaterService && services.stream().noneMatch(currClass -> Service.class.isAssignableFrom(currClass) || currClass.getName().equalsIgnoreCase(Service.class.getName()))) {
            services.add(Service.class);
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

    private void registerComponent(List<Class<?>> componentClasses, Object service, FrameworkComponent frameworkComponentAnnotation, boolean isPrimary, ComponentRegistry registry, Dictionary<String, Object> dictionary) {
        componentClasses.forEach(componentClass -> {
            try {
                //framework component is registered with the given priority
                ComponentConfiguration componentConfiguration = ComponentConfigurationFactory.createNewComponentPropertyFactory()
                        .withPriority(frameworkComponentAnnotation.priority())
                        //set primary the component with the highest priority
                        .setPrimary(isPrimary)
                        .fromStringDictionary(dictionary)
                        .build();
                ComponentRegistration<T, K> registration = (ComponentRegistration<T, K>) registry.registerComponent(componentClass, service, componentConfiguration);
                if (registration.getComponent() != null) {
                    registeredServices.add(registration);
                    log.debug("Component: {} succesfully registered!", componentClass.getName());
                    log.debug("Searching for init method...");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }
}
