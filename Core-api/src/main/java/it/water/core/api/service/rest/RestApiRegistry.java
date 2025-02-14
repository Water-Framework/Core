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

import it.water.core.api.service.Service;

import java.util.Map;

/**
 * @Author Aristide Cittadino.
 * Component registering all rest services available in the current runtime.
 */
public interface RestApiRegistry extends Service {
    /**
     * Adds a new rest api service
     *
     * @param restApiInterface Generic RestApi interface
     * @param concreteClass    Concrete implementation of the Rest Api
     */
    void addRestApiService(Class<? extends RestApi> restApiInterface, Class<? extends RestApi> concreteClass);

    /**
     * Remove, if exists, restApi interface
     *
     * @param restApiInterface Generic RestApi interface
     */
    void removeRestApiService(Class<? extends RestApi> restApiInterface);

    /**
     * @param restApi generic RestApi interface
     * @return the current implementation class
     */
    Class<?> getRestApiImplementation(Class<? extends RestApi> restApi);

    /**
     * @return copy of the current registry
     */
    Map<Class<? extends RestApi>, Class<?>> getRegisteredRestApis();

    /**
     * Request for a restart to the current rest api manager if any
     */
    void sendRestartApiManagerRestartRequest();
}
