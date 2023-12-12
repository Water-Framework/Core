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
package it.water.core.service.service;

import it.water.core.api.permission.SecurityContext;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.service.BaseServiceImpl;
import it.water.core.service.api.TestServiceApi;
import it.water.core.service.api.TestSystemServiceApi;
import lombok.Getter;
import lombok.Setter;

/**
 * Test service just to verifiy bean creation.
 * This service is not associated with an entity it is an integration service.
 */
@FrameworkComponent
public class TestServiceImpl extends BaseServiceImpl implements TestServiceApi {
    @Inject
    @Setter
    @Getter
    private TestSystemServiceApi systemService;

    @Override
    protected SecurityContext getSecurityContext() {
        return null;
    }

    @Override
    public void doSomething() {
        this.getSystemService().doSomething();
    }
}
