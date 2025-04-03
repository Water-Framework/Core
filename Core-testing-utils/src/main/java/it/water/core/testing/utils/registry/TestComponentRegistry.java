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

import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.api.service.Service;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.registry.AbstractComponentRegistry;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.testing.utils.filter.TestComponentFilterBuilder;
import it.water.core.testing.utils.interceptors.TestServiceProxy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

public class TestComponentRegistry extends AbstractComponentRegistry implements ComponentRegistry {
    private Map<Class<?>, List<ComponentRegistration<?, TestComponentRegistration<?>>>> registrations;

    private Map<String, BaseEntitySystemApi<?>> baseEntitySystemApis;
    private Map<String, BaseRepository<?>> baseRepositories;
    private Map<String, Integer> baseEntitySystemApiPriority;
    private Map<String, Integer> baseRepositoriesPriority;

    public TestComponentRegistry() {
        this.registrations = new HashMap<>();
        this.baseEntitySystemApis = new HashMap<>();
        this.baseRepositories = new HashMap<>();
        this.baseRepositoriesPriority = new HashMap<>();
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
            getGenericClasses(toClass, toClassList);
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
            } else {
                toRegister = component;
            }
            //registration
            ComponentRegistration<T, K> registration = doRegistration(componentClass, toRegister, configuration);

            if (BaseEntitySystemApi.class.isAssignableFrom(component.getClass())) {
                BaseEntitySystemApi<?> entitySystemApi = (BaseEntitySystemApi) component;
                String entityType = entitySystemApi.getEntityType().getName();
                baseEntitySystemApis.computeIfAbsent(entityType, key -> (BaseEntitySystemApi<?>) toRegister);
                baseEntitySystemApiPriority.computeIfAbsent(entityType, key -> configuration.getPriority());
                long currentPriority = baseEntitySystemApiPriority.get(entityType);
                //updating system apis with the one with highest priority
                if (configuration.getPriority() > currentPriority) {
                    baseEntitySystemApis.put(entityType, entitySystemApi);
                    baseEntitySystemApiPriority.put(entityType, configuration.getPriority());
                }
            }

            if (BaseRepository.class.isAssignableFrom(component.getClass())) {
                BaseRepository<?> baseRepository = (BaseRepository<?>) component;
                String entityType = baseRepository.getEntityType().getName();
                baseRepositories.computeIfAbsent(entityType, key -> (BaseRepository<?>) toRegister);
                baseRepositoriesPriority.computeIfAbsent(entityType, key -> configuration.getPriority());
                long currentPriority = baseRepositoriesPriority.get(entityType);
                //updating system apis with the one with highest priority
                if (configuration.getPriority() > currentPriority) {
                    baseRepositories.put(entityType, baseRepository);
                    baseRepositoriesPriority.put(entityType, configuration.getPriority());
                }
            }
            //if it is water service we set the registration inside the proxy itself
            if (proxy != null) {
                proxy.setRegistration((TestComponentRegistration) registration);
            }
            return registration;
        }
        throw new WaterRuntimeException("Registration component cannot be null");
    }

    void getGenericClasses(Type[] toClass, List<Class<?>> toClassList) {
        for (int i = 0; i < toClass.length; i++) {
            Type t = toClass[i];
            if (t instanceof ParameterizedType) {
                Class<?> toExposeClass = (Class<?>) ((ParameterizedType) t).getRawType();
                toClassList.add(toExposeClass);
                Type[] ancestorInterfaces = toExposeClass.getGenericInterfaces();
                if (ancestorInterfaces != null && ancestorInterfaces.length > 0)
                    getGenericClasses(ancestorInterfaces, toClassList);
            } else
                toClassList.add((Class<?>) t);
        }
    }

    private <T, K> ComponentRegistration<T, K> doRegistration(Class<?> componentClass, Object toRegister, ComponentConfiguration configuration) {
        this.registrations.computeIfAbsent(componentClass, k -> new ArrayList<>());
        ComponentRegistration<?, K> registration = (ComponentRegistration<T, K>) new TestComponentRegistration<>(componentClass, toRegister, configuration);
        this.registrations.get(componentClass).add((ComponentRegistration<?, TestComponentRegistration<?>>) registration);
        return (ComponentRegistration<T, K>) registration;
    }

    @Override
    public <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration) {
        this.invokeLifecycleMethod(OnDeactivate.class, registration.getComponent().getClass(), registration.getComponent());
        return removeComponentFromRegistry(registration.getRegistrationClass(), registration.getComponent());
    }

    @Override
    public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
        this.invokeLifecycleMethod(OnDeactivate.class, componentClass, component);
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

    @Override
    public <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName) {
        return (T) baseEntitySystemApis.get(entityClassName);
    }

    @Override
    public <T extends BaseRepository> T findEntityRepository(String entityClassName) {
        return (T) baseRepositories.get(entityClassName);
    }

    private <T> List<T> filterComponents(Class<T> componentClass, ComponentFilter filter) {
        //Ordering found components by priority, the first one is the one with the highest priority
        TreeMap<Integer, List<T>> foundComponents = new TreeMap<>();
        this.registrations.get(componentClass).forEach(registration -> {
            foundComponents.computeIfAbsent(registration.getConfiguration().getPriority(), key -> new ArrayList<>());
            if (filter == null || filter.matches(registration.getConfiguration().getConfiguration()))
                foundComponents.get(registration.getConfiguration().getPriority()).add((T) registration.getComponent());
        });
        List<T> foundComponentsOrdered = new ArrayList<>();
        foundComponents.descendingMap().values().stream().flatMap(Collection::stream).forEach(foundComponentsOrdered::add);
        return foundComponentsOrdered;
    }
}
