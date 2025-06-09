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

import it.water.core.api.model.Resource;
import it.water.core.api.validation.WaterValidator;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.service.api.TestSystemServiceApi;
import lombok.Setter;

/**
 * Test service just to verifiy bean creation.
 * This service is not associated with an entity it is an integration service.
 */
public class GenericSystemServiceImpl extends it.water.core.service.GenericSystemServiceImpl implements TestSystemServiceApi {

    @Inject
    @Setter
    private WaterValidator waterValidator;

    public void checkValidate(Resource resource) {
        this.validate(resource);
    }

    @Override
    protected WaterValidator getValidatorInstance() {
        return waterValidator;
    }

    @Override
    public void doSomething() {
        getLog().info("do something");
    }


}
