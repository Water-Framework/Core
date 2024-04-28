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

import it.water.core.api.action.ActionsManager;
import it.water.core.api.model.Resource;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.FrameworkRestController;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.permission.annotations.AccessControl;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import lombok.Getter;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Aristide Cittadino
 * Layer of abstraction which encapsulates the logic of initialize an water based environment.
 * Every framework that supports Framework should rely on hierarchy of this class to setup all needed components.
 */
public abstract class AbstractInitializer<T, K> {
    private static Logger log = LoggerFactory.getLogger(AbstractInitializer.class);
    @Getter
    protected List<ComponentRegistration<T, K>> registeredServices;

    protected AbstractInitializer() {
        registeredServices = new ArrayList<>();
    }


    /**
     * Returns the specific component registry
     *
     * @return
     */
    protected abstract ComponentRegistry getComponentRegistry();

    /**
     * Loads all framework components declared inside the application or runtime
     */
    protected void initializeFrameworkComponents() {
        this.setupFrameworkComponents(getAnnotatedClasses(FrameworkComponent.class));
    }

    /**
     * Instantiates all componente inside the application or runtime
     *
     * @param framworkComponents
     */
    protected abstract void setupFrameworkComponents(Iterable<Class<?>> framworkComponents);

    /**
     * This method register all access controlled entities with permission
     *
     * @param <N>
     */
    protected <N extends Resource> void initializeResourcePermissionsAndActions() {
        Iterable<Class<?>> accessControlledClasses = getAnnotatedClasses(AccessControl.class);
        accessControlledClasses.forEach(accessControlledClass -> {
            ActionsManager manager = this.getComponentRegistry().findComponent(ActionsManager.class, null);
            try {
                if (Resource.class.isAssignableFrom(accessControlledClass)) {
                    Class<N> resourceClass = (Class<N>) accessControlledClass;
                    manager.registerActions(resourceClass);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    /**
     * Register all @FrameworkRestApi classes as rest services.
     * This method calls registerRestApis which will be implementend for each available runtime.
     */
    protected void initializeRestApis() {
        //registering rest apis if any
        try {
            RestApiRegistry restApiRegistry = getComponentRegistry().findComponent(RestApiRegistry.class, null);
            //discover all concrete implementation for every defined rest api
            this.setupRestApis(getAnnotatedClasses(FrameworkRestController.class), restApiRegistry);
        } catch (NoComponentRegistryFoundException e) {
            log.warn("No Rest API Manager found, skipping rest api automatic registration...");
        }
    }

    /**
     * @param frameworkRestApis list of @FrameworkRestApi
     * There are 3 main objects for rest api definitions.
     * Generic Rest Api - defines only swagger documentation and methods that a rest API should expose
     * Concrete Rest Api - it's the interface in a specific technology which extends the previous one ex. Jax RS interface with related annotations
     * Concrete Implementations - Class which implements the generic interface but must be exposed following the concrete. This binding is done dinamically
     */
    protected void setupRestApis(Iterable<Class<?>> frameworkRestApis, RestApiRegistry restApiRegistry) {
        //register to rest api manager every service
        frameworkRestApis.forEach(restApiService -> {
            Optional<Annotation> frameworkRestControllerAnnotationOpt = Arrays.stream(restApiService.getAnnotations()).filter(annotation -> annotation.annotationType().getName().equalsIgnoreCase(FrameworkRestController.class.getName())).findAny();
            if (frameworkRestControllerAnnotationOpt.isPresent()) {
                FrameworkRestController frameworkRestControllerAnnotation = (FrameworkRestController) frameworkRestControllerAnnotationOpt.get();
                //register APIs only if it finds a rest Api Manager
                if (restApiRegistry != null) {
                    //add a rest api service passing all registered Rest Apis in order to find the right one
                    Class<? extends RestApi> genericRestApi = frameworkRestControllerAnnotation.referredRestApi();
                    Class<? extends RestApi> concreteRestApi = (Class<? extends RestApi>) findConcreteRestApi(getAnnotatedClasses(FrameworkRestApi.class), genericRestApi);
                    restApiRegistry.addRestApiService(concreteRestApi, (Class<? extends RestApi>) restApiService);
                }
            }
        });
    }

    private Class<?> findConcreteRestApi(Iterable<Class<?>> iterableFrameworkRestApis, Class<? extends RestApi> crossFrameworkRestApi) {
        AtomicReference<Class<?>> foundConcreteRestApiClass = new AtomicReference<>();
        iterableFrameworkRestApis.forEach(restApi -> {
            if (restApi.isInterface() && crossFrameworkRestApi.isAssignableFrom(restApi)) {
                foundConcreteRestApiClass.set(restApi);
                return;
            }
        });
        if(foundConcreteRestApiClass.get() == null){
            //if there's no specialization , let's return the cross framework api with is supposed to expose jaxrs annotations
            return crossFrameworkRestApi;
        }
        return foundConcreteRestApiClass.get();
    }


    /**
     * Default implementation using ClassIndex.
     * This method can be overridden in order to implement different discover mechanisms.
     *
     * @param annotation Annotation that has to be searched
     * @return Iterable of annotated classes
     */
    protected Iterable<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return ClassIndex.getAnnotated(annotation, this.getCurrentClassLoader());
    }

    /**
     * @return current class loader base on each runtime, default class loader is taken by the current instance.
     */
    protected ClassLoader getCurrentClassLoader() {
        return this.getClass().getClassLoader();
    }
}
