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
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.validation.WaterValidator;
import it.water.core.service.api.TestServiceApi;
import it.water.core.service.api.TestSystemServiceApi;
import it.water.core.service.integration.discovery.ServiceDiscoveryRegistryClientImpl;
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

import java.util.Properties;

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
        DiscoverableServiceInfo serviceInfo = new DiscoverableServiceInfo() {
            @Override
            public String getServiceProtocol() {
                return "http";
            }

            @Override
            public String getServicePort() {
                return "8080";
            }

            @Override
            public String getServiceId() {
                return "1";
            }

            @Override
            public String getServiceInstanceId() {
                return "1";
            }

            @Override
            public String getServiceRoot() {
                return "/water";
            }
        };
        server.registerService(serviceInfo);
        Assertions.assertNull(server.getServiceInfo("id"));
        server.unregisterService("id");
        Assertions.assertNull(server.getServiceInfo("id"));
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        Assertions.assertDoesNotThrow(() -> client.registerService(serviceInfo));
        Assertions.assertDoesNotThrow(() -> client.getServiceInfo("id"));
        Assertions.assertDoesNotThrow(() -> client.unregisterService("id"));
        Assertions.assertDoesNotThrow(() -> client.setup("rmeote","port"));

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

    @Test
    void testApplicationProperties(){
        ApplicationProperties applicationProperties = initializer.getComponentRegistry().findComponent(ApplicationProperties.class, null);
        Assertions.assertNotNull(applicationProperties);
        Properties props = new Properties();
        props.put("propA","valueA");
        props.put("propB","valueB");
        props.put("propNumber",3L);
        props.put("propBoolean",true);
        applicationProperties.loadProperties(props);
        Assertions.assertEquals("valueA",applicationProperties.getProperty("propA"));
        Assertions.assertEquals("valueA",applicationProperties.getPropertyOrDefault("propA","nothing"));
        Assertions.assertEquals(3L,applicationProperties.getPropertyOrDefault("propNumber",10L));
        Assertions.assertTrue(applicationProperties.getPropertyOrDefault("propBoolean",false));
        Assertions.assertEquals("valueC",applicationProperties.getPropertyOrDefault("propC","valueC"));
        Assertions.assertEquals(4L,applicationProperties.getPropertyOrDefault("propC",4L));
        Assertions.assertFalse(applicationProperties.getPropertyOrDefault("propC",false));

    }
}
