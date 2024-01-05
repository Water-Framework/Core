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
package it.water.core.api.service.rest;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Aristide Cittadino
 * This class maps the concept of Rest Api Manager.
 * Basically this component will regiter speficic classes inside the specific jaxrs server running
 * on a specific technology ex. spring,osgi,quarkus.
 * Rest services must be registered through generic interfaces which define specific implementations (Jax RS, Spring).
 */
public interface RestApiManager extends Service {
    /**
     * Adds a rest api implementation starting from a generic interface.
     *
     * @param restApi
     * @param service
     */
    void addRestApiService(Class<? extends RestApi> restApi, Class<?> service);

    /**
     * Defines all annotated @FrameworkRestApi in order to discover the concrete implementations
     *
     * @param registeredConcreteApis
     */
    void setAnnotatedRestApis(Iterable<Class<?>> registeredConcreteApis);

    /**
     * Removes a rest api implementation starting from a generic interface.
     *
     * @param restApi
     */
    void removeRestApiService(Class<? extends RestApi> restApi);

    /**
     * Retrieving the previous registered implementation for the specified rest api
     *
     * @param restApi
     * @return
     */
    Class<?> getRestImplementation(Class<? extends RestApi> restApi);

    /**
     * Returns the list of all registered rest APIs
     *
     * @return
     */
    Set<Class<? extends RestApi>> getRegisteredApis();

    /**
     * Method invoked at startup cannot be injected by interceptors
     * So the Abstract initializer explicitly call this method to set the component registry
     * inside the Api Manager.
     *
     * @param componentRegistry
     */
    void setComponentRegistry(ComponentRegistry componentRegistry);


    /**
     * Start the server implementation for rest APIs
     */
    void startRestApiServer();

    /**
     * Stops the server implementatioj for rest APIs
     */
    void stopRestApiServer();

    default Class<?> findConcreteRestApi(Iterable<Class<?>> iterableFrameworkRestApis, Class<? extends RestApi> crossFrameworkRestApi) {
        AtomicReference<Class<?>> foundConcreteRestApiClass = new AtomicReference<>();
        iterableFrameworkRestApis.forEach(restApi -> {
            if (restApi.isInterface() && crossFrameworkRestApi.isAssignableFrom(restApi)) {
                foundConcreteRestApiClass.set(restApi);
                return;
            }
        });
        return foundConcreteRestApiClass.get();
    }
}
