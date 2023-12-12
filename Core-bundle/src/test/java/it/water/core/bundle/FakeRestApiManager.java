/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.core.bundle;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiManager;

import java.util.Set;

public class FakeRestApiManager implements RestApiManager {
    @Override
    public void addRestApiService(Class<? extends RestApi> restApi, Class<?> service) {
        //do nothing
    }

    @Override
    public void setAnnotatedRestApis(Iterable<Class<?>> registeredConcreteApis) {
        //do nothing
    }

    @Override
    public void removeRestApiService(Class<? extends RestApi> restApi) {
        //do nothing
    }

    @Override
    public Class<?> getRestImplementation(Class<? extends RestApi> restApi) {
        return null;
    }

    @Override
    public Set<Class<? extends RestApi>> getRegisteredApis() {
        return null;
    }

    @Override
    public void setComponentRegistry(ComponentRegistry componentRegistry) {
        //do nothing
    }

    @Override
    public void startRestApiServer() {
        //do nothing
    }

    @Override
    public void stopRestApiServer() {
        //do nothing
    }
}
