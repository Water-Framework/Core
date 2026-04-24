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

package it.water.core.service.integration.discovery;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.api.service.integration.discovery.ServiceLivenessClient;
import it.water.core.api.service.integration.discovery.ServiceLivenessListener;
import it.water.core.api.service.integration.discovery.ServiceLivenessRegistration;
import it.water.core.api.service.integration.discovery.ServiceLivenessSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class DescriptorDrivenServiceRegistrationLifecycleManagerImplTest {

    @Test
    void activatesAndDeactivatesRegistrationsFromDescriptor() throws Exception {
        Path descriptorDirectory = Files.createTempDirectory("water-descriptor-test");
        Path metaInfDirectory = descriptorDirectory.resolve("META-INF");
        Files.createDirectories(metaInfDirectory);
        Files.writeString(metaInfDirectory.resolve("water-descriptor.json"), """
                {
                  "schemaVersion": "1.1",
                  "moduleId": "it.water.test.service",
                  "runtime": {
                    "serviceRegistration": {
                      "enabled": true,
                      "serviceName": "descriptor-service",
                      "root": "/water/descriptors",
                      "serviceVersion": "3.0.0",
                      "protocol": "http"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{descriptorDirectory.toUri().toURL()},
                getClass().getClassLoader())) {
            DescriptorDrivenServiceRegistrationLifecycleManagerImpl manager =
                    new DescriptorDrivenServiceRegistrationLifecycleManagerImpl();
            InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
            RecordingRegistryClient discoveryClient = new RecordingRegistryClient();
            RecordingLivenessClient livenessClient = new RecordingLivenessClient();
            MapApplicationProperties applicationProperties = new MapApplicationProperties();
            applicationProperties.put(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "http://127.0.0.1:8181/water");
            applicationProperties.put("org.osgi.service.http.port", "8381");

            registry.register(ServiceDiscoveryGlobalOptions.class,
                    new FixedGlobalOptions("http://127.0.0.1:8181/water", "127.0.0.1"));
            registry.register(ServiceDiscoveryRegistryClientInternal.class, discoveryClient);
            registry.register(ServiceLivenessClient.class, livenessClient);

            manager.activateDescriptorRegistrations(registry, applicationProperties, classLoader);

            Assertions.assertNotNull(discoveryClient.registeredInfo);
            Assertions.assertEquals("descriptor-service", discoveryClient.registeredInfo.getServiceId());
            Assertions.assertEquals("8381", discoveryClient.registeredInfo.getServicePort());
            Assertions.assertNotNull(livenessClient.lastRegistration);
            Assertions.assertEquals("descriptor-service", livenessClient.lastRegistration.getServiceName());

            manager.deactivate();

            Assertions.assertEquals("descriptor-service", discoveryClient.unregisteredServiceName);
            Assertions.assertTrue(livenessClient.stopped);
        }
    }

    private static final class RecordingRegistryClient implements ServiceDiscoveryRegistryClientInternal {
        private DiscoverableServiceInfoImpl registeredInfo;
        private String unregisteredServiceName;

        @Override
        public void registerService(DiscoverableServiceInfo registration) {
            this.registeredInfo = (DiscoverableServiceInfoImpl) registration;
        }

        @Override
        public void unregisterService(String serviceName, String instanceId) {
            this.unregisteredServiceName = serviceName;
            this.registeredInfo = null;
        }

        @Override
        public DiscoverableServiceInfo getServiceInfo(String id) {
            return registeredInfo;
        }

        @Override
        public void setup(String remoteUrl, String port) {
        }

        @Override
        public boolean isRegistered(String instanceId) {
            return registeredInfo != null && instanceId.equals(registeredInfo.getServiceInstanceId());
        }

        @Override
        public boolean heartbeat(String serviceName, String instanceId) {
            return registeredInfo != null
                    && serviceName.equals(registeredInfo.getServiceId())
                    && instanceId.equals(registeredInfo.getServiceInstanceId());
        }
    }

    private static final class RecordingLivenessClient implements ServiceLivenessClient {
        private ServiceLivenessRegistration lastRegistration;
        private boolean stopped;

        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            this.lastRegistration = registration;
            this.stopped = false;
            return () -> stopped = true;
        }
    }

    private static final class FixedGlobalOptions implements ServiceDiscoveryGlobalOptions {
        private final String discoveryUrl;
        private final String defaultHost;

        private FixedGlobalOptions(String discoveryUrl, String defaultHost) {
            this.discoveryUrl = discoveryUrl;
            this.defaultHost = defaultHost;
        }

        @Override
        public String getDiscoveryUrl() {
            return discoveryUrl;
        }

        @Override
        public String getDefaultHost() {
            return defaultHost;
        }

        @Override
        public long getHeartbeatIntervalSeconds() {
            return 25L;
        }

        @Override
        public long getRegistrationRetryInitialDelaySeconds() {
            return 30L;
        }

        @Override
        public long getRegistrationRetryMaxDelaySeconds() {
            return 300L;
        }

        @Override
        public long getHttpTimeoutSeconds() {
            return 10L;
        }

        @Override
        public int getRegistrationMaxAttempts() {
            return 3;
        }

        @Override
        public long[] getRegistrationRetryBackoffMs() {
            return new long[]{2000L, 4000L, 8000L};
        }
    }

    private static final class MapApplicationProperties implements ApplicationProperties {
        private final Map<String, Object> properties = new HashMap<>();

        void put(String key, Object value) {
            properties.put(key, value);
        }

        @Override
        public void setup() {
        }

        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            return properties.containsKey(key);
        }

        @Override
        public void loadProperties(File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void loadProperties(Properties props) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unloadProperties(File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unloadProperties(Properties props) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryComponentRegistry implements ComponentRegistry {
        private final Map<Class<?>, Object> components = new HashMap<>();

        <T> void register(Class<T> componentClass, T component) {
            components.put(componentClass, component);
        }

        @Override
        public <T> List<T> findComponents(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            T component = findComponent(componentClass, filter);
            return component == null ? Collections.emptyList() : Collections.singletonList(component);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T findComponent(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            return (T) components.get(componentClass);
        }

        @Override
        public <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComponentFilterBuilder getComponentFilterBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseRepository> T findEntityRepository(String entityClassName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseEntity> BaseRepository<T> findEntityExtensionRepository(Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }
}
