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
package it.water.core.service;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.service.Service;
import it.water.core.api.service.integration.discovery.ServiceInfo;
import it.water.core.api.validation.WaterValidator;
import it.water.core.service.api.TestServiceApi;
import it.water.core.service.api.TestSystemServiceApi;
import it.water.core.service.integration.discovery.ServiceDiscoveryRegistryInMemoryServer;
import it.water.core.service.service.ConcreteBaseService;
import it.water.core.service.service.ConcreteService;
import it.water.core.service.service.GenericSystemServiceImpl;
import it.water.core.service.service.TestResoruce;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.testing.utils.junit.WaterTestExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WaterServiceTest implements Service {

    private TestRuntimeInitializer initializer;

    @BeforeAll
    public void initializeTestFramework() {
        initializer = TestRuntimeInitializer.getInstance();
    }

    /**
     * Checking wether all framework components have been initialized correctly
     */
    @Test
    void testRegisteredComponents() {
        Assertions.assertNotNull(initializer.getComponentRegistry());
        Assertions.assertNotNull(initializer.getComponentRegistry().findComponents(ApplicationProperties.class, null));
        Assertions.assertNotNull(initializer.getComponentRegistry().findComponents(TestServiceApi.class, null));
    }

    @Test
    void testIntegrationServices() {
        TestServiceApi testServiceApi = initializer.getComponentRegistry().findComponent(TestServiceApi.class, null);
        Assertions.assertDoesNotThrow(() -> testServiceApi.doSomething());
    }

    @Test
    void serviceDiscoveryTest(){
        ServiceDiscoveryRegistryInMemoryServer server = new ServiceDiscoveryRegistryInMemoryServer();
        ServiceInfo serviceInfo = new ServiceInfo() {
            @Override
            public String getId() {
                return "id";
            }

            @Override
            public String getProtocol() {
                return "http";
            }

            @Override
            public String getIp() {
                return "10.12.23.12";
            }

            @Override
            public String getHost() {
                return "sample.com";
            }

            @Override
            public String getPort() {
                return "8080";
            }

            @Override
            public String getContextRoot() {
                return "/sample";
            }

            @Override
            public String getRelativePath() {
                return "/sample";
            }
        };
        server.registerService(serviceInfo);
        Assertions.assertNotNull(server.getServiceInfo("id"));
        server.unregisterService("id");
        Assertions.assertNull(server.getServiceInfo("id"));
    }

    @Test
    void testBasicServices(){
        WaterValidator waterValidator = initializer.getComponentRegistry().findComponent(WaterValidator.class,null);
        TestSystemServiceApi testSystemServiceApi = initializer.getComponentRegistry().findComponent(TestSystemServiceApi.class, null);
        TestServiceApi testServiceApi = initializer.getComponentRegistry().findComponent(TestServiceApi.class, null);
        TestResoruce testResoruce = new TestResoruce();
        GenericSystemServiceImpl genericSystemService = new GenericSystemServiceImpl();
        genericSystemService.setWaterValidator(waterValidator);
        Assertions.assertDoesNotThrow(() -> genericSystemService.checkValidate(testResoruce));
        Assertions.assertDoesNotThrow(() -> testSystemServiceApi.checkValidate(testResoruce));
        Assertions.assertNotNull(testServiceApi.checkSystemApi());
        ConcreteService concreteService = new ConcreteService();
        Assertions.assertNotNull(concreteService);
        ConcreteBaseService concreteBaseService = new ConcreteBaseService();
        Assertions.assertNotNull(concreteBaseService);
        Assertions.assertNull(concreteBaseService.checkSystemService());
    }
}
