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
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.validation.WaterValidator;
import it.water.core.service.api.TestServiceApi;
import it.water.core.service.api.TestSystemServiceApi;
import it.water.core.service.integration.discovery.DiscoverableServiceInfoImpl;
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
        DiscoverableServiceInfoImpl serviceInfo = new DiscoverableServiceInfoImpl(
                "http", "8080", "1", "1", "/water", "1.0.0", "localhost", null
        );
        server.registerService(serviceInfo);
        Assertions.assertNotNull(server.getServiceInfo("1"));
        server.unregisterService("1", "1");
        Assertions.assertNull(server.getServiceInfo("1"));
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // setup() must be called before any operation
        Assertions.assertDoesNotThrow(() -> client.setup("http://localhost:19999/water", "8080"));
        // registerService will fail to connect (no server running) but should not throw unchecked exceptions
        Assertions.assertDoesNotThrow(() -> client.registerService(serviceInfo));
        Assertions.assertDoesNotThrow(() -> client.getServiceInfo("id"));
        Assertions.assertDoesNotThrow(() -> client.unregisterService("1", "1"));

    }

    @Test
    void testClusterNodeOptions(){
        ClusterNodeOptions clusterNodeOptions = initializer.getComponentRegistry().findComponent(ClusterNodeOptions.class, null);
        Assertions.assertFalse(clusterNodeOptions.clusterModeEnabled());
        Assertions.assertEquals("water-node-0",clusterNodeOptions.getNodeId());
        Assertions.assertEquals("microservices",clusterNodeOptions.getLayer());
        Assertions.assertEquals("127.0.0.1",clusterNodeOptions.getIp());
        Assertions.assertEquals("localhost",clusterNodeOptions.getHost());
        Assertions.assertEquals(false,clusterNodeOptions.useIpInClusterRegistration());
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
