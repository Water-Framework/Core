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
package it.water.core.service.service;

import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.model.Resource;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.LogMethodExecution;
import it.water.core.service.BaseSystemServiceImpl;
import it.water.core.service.api.TestServiceApi;
import it.water.core.service.api.TestSystemServiceApi;

/**
 * Test service just to verifiy bean creation.
 * This service is not associated with an entity it is an integration service.
 */
@FrameworkComponent
public class TestSystemServiceImpl extends BaseSystemServiceImpl implements TestSystemServiceApi {
    @Override
    protected void validate(Resource resource) {
        //do nothing
        super.validate(resource);
    }

    @OnActivate
    public void onActivate(TestServiceApi service) {
        //do nothing
    }

    @LogMethodExecution
    public void checkValidate(Resource resource) {
        this.validate(resource);
    }

    @Override
    public void doSomething() {
        getLog().info("do something");
    }
}
