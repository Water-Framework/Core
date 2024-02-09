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
package it.water.core.testing.utils.registry;

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.service.Service;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.testing.utils.filter.TestComponentFilterBuilder;
import it.water.core.testing.utils.interceptors.TestServiceProxy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

public class TestComponentRegistry implements ComponentRegistry {
    private Map<Class<?>, List<ComponentRegistration<?, TestComponentRegistration<?>>>> registrations;

    public TestComponentRegistry() {
        this.registrations = new HashMap<>();
    }

    @Override
    public <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter) {
        if (this.registrations.containsKey(componentClass))
            return filterComponents(componentClass, filter);
        throw new NoComponentRegistryFoundException();
    }

    @Override
    public <T> T findComponent(Class<T> componentClass, ComponentFilter filter) {
        List<T> components = findComponents(componentClass, filter);
        return (components != null && !components.isEmpty()) ? components.get(0) : null;
    }

    @Override
    public <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration) {
        if (component != null) {
            final Object toRegister;
            Type[] toClass = component.getClass().getGenericInterfaces();
            List<Class<?>> toClassList = new ArrayList<>();
            getGenericClasses(toClass,toClassList);
            if (componentClass.isInterface() && !toClassList.contains(componentClass))
                toClassList.add(componentClass);
            //forcing to be a proxy
            toClassList.add(it.water.core.api.interceptors.Proxy.class);
            TestServiceProxy<?> proxy = null;
            if (Service.class.isAssignableFrom(component.getClass())) {
                toClass = new Class[toClassList.size()];
                Class<?>[] interfacesWithGenerics = new Class<?>[toClass.length];
                proxy = new TestServiceProxy<>((Service) component, this);
                Object o = Proxy.newProxyInstance(this.getClass().getClassLoader(), toClassList.toArray(interfacesWithGenerics), proxy);
                toRegister = o;

            } else
                toRegister = component;
            ComponentRegistration<T, K> registration = doRegistration(componentClass, toRegister, configuration);
            //if it is water service we set the registration inside the proxy itself
            if (proxy != null) {
                proxy.setRegistration((TestComponentRegistration) registration);
            }
            return registration;
        }
        throw new WaterRuntimeException("Registration component cannot be null");
    }

    void getGenericClasses(Type[] toClass,List<Class<?>> toClassList){
        for (int i = 0; i < toClass.length; i++) {
            Type t = toClass[i];
            if (t instanceof ParameterizedType) {
                Class<?> toExposeClass = (Class<?>) ((ParameterizedType) t).getRawType();
                toClassList.add(toExposeClass);
                Type[] ancestorInterfaces = toExposeClass.getGenericInterfaces();
                if(ancestorInterfaces != null && ancestorInterfaces.length > 0)
                    getGenericClasses(ancestorInterfaces,toClassList);
            } else
                toClassList.add((Class<?>) t);
        }
    }

    private <T, K> ComponentRegistration<T, K> doRegistration(Class<?> componentClass, Object toRegister, ComponentConfiguration configuration) {
        this.registrations.computeIfAbsent(componentClass, k -> new ArrayList<>());
        Properties props = (configuration != null && configuration.getConfiguration() != null) ? configuration.getConfiguration() : new Properties();
        ComponentRegistration<?, K> registration = (ComponentRegistration<T, K>) new TestComponentRegistration<>(componentClass, toRegister, props);
        this.registrations.get(componentClass).add((ComponentRegistration<?, TestComponentRegistration<?>>) registration);
        return (ComponentRegistration<T, K>) registration;
    }

    @Override
    public <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration) {
        return removeComponentFromRegistry(registration.getRegistrationClass(), registration.getComponent());
    }

    @Override
    public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
        return removeComponentFromRegistry(componentClass, component);
    }

    private <T> boolean removeComponentFromRegistry(Class<? extends T> componentClass, T component) {
        if (this.registrations.containsKey(componentClass)) {
            if (!Proxy.isProxyClass(component.getClass())) {
                Optional<ComponentRegistration<?, TestComponentRegistration<?>>> optionalRegistration = this.registrations.get(componentClass).stream().filter(configuration -> configuration.getComponent().equals(component)).findAny();
                if (optionalRegistration.isPresent()) {
                    return this.registrations.get(componentClass).remove(optionalRegistration.get());
                }
            } else {
                return doRemoveComponentProxy(componentClass, component);
            }
        }
        return false;
    }

    private <T> boolean doRemoveComponentProxy(Class<? extends T> componentClass, T component) {
        int indexFound = -1;
        for (int i = 0; i < this.registrations.get(componentClass).size() && indexFound == -1; i++) {
            Object currComponent = this.registrations.get(componentClass).get(i).getComponent();
            if (Proxy.isProxyClass(currComponent.getClass())) {
                Object currComponentObject = Proxy.getInvocationHandler(currComponent);
                Object componentObject = Proxy.getInvocationHandler(component);
                if (currComponentObject == componentObject) {
                    indexFound = i;
                }
            }
        }
        if (indexFound >= 0) {
            this.registrations.get(componentClass).remove(indexFound);
            return true;
        }
        return false;
    }

    @Override
    public ComponentFilterBuilder getComponentFilterBuilder() {
        return new TestComponentFilterBuilder();
    }

    private <T> List<T> filterComponents(Class<T> componentClass, ComponentFilter filter) {
        List<T> foundComponents = new ArrayList<>();
        this.registrations.get(componentClass).forEach(registration -> {
            if (filter == null || filter.matches(registration.getConfiguration().getConfiguration()))
                foundComponents.add((T) registration.getComponent());
        });
        return foundComponents;
    }
}
