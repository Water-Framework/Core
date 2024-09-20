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

import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.api.service.rest.RestApiRegistry;

import java.util.Map;

public class FakeRestApiRegistry implements RestApiRegistry {

    @Override
    public void addRestApiService(Class<? extends RestApi> restApiInterface, Class<? extends RestApi> concreteClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRestApiService(Class<? extends RestApi> restApiInterface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getRestApiImplementation(Class<? extends RestApi> restApi) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Class<? extends RestApi>, Class<?>> getRegisteredRestApis() {
        throw new UnsupportedOperationException();
    }
}
