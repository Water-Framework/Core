/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.service.integration.discovery.ServiceLivenessClient;
import it.water.core.api.service.integration.discovery.ServiceLivenessListener;
import it.water.core.api.service.integration.discovery.ServiceLivenessRegistration;
import it.water.core.api.service.integration.discovery.ServiceLivenessSession;
import it.water.core.api.service.integration.discovery.ServiceRegistrationOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ServiceRegistrationLifecycleSupportTest {

    @Test
    void registersWithModuleScopedOptionsEvenWhenGlobalOptionsAreMissing() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water",
                "catalog-service",
                "1.0.0",
                "",
                "http",
                "/water/catalog",
                "",
                "8381",
                "127.0.0.1"
        );

        support.register(client, options, livenessClient);

        Assertions.assertEquals("http://127.0.0.1:8181/water", client.setupRemoteUrl);
        Assertions.assertEquals("8381", client.setupPort);
        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertNotNull(livenessClient.lastRegistration);
        Assertions.assertEquals("catalog-service", client.registeredInfo.getServiceId());
        Assertions.assertEquals("8381", client.registeredInfo.getServicePort());
        Assertions.assertEquals("/water/catalog", client.registeredInfo.getServiceRoot());
        Assertions.assertEquals("127.0.0.1", client.registeredInfo.getServiceHost());
        Assertions.assertEquals("catalog-service", support.currentRegisteredServiceName());
        Assertions.assertNotNull(support.currentRegisteredInstanceId());
        Assertions.assertTrue(support.currentRegisteredInstanceId().startsWith("catalog-service-"));

        String registeredInstanceId = support.currentRegisteredInstanceId();
        support.deregister(client);

        Assertions.assertEquals("catalog-service", client.unregisteredServiceName);
        Assertions.assertEquals(registeredInstanceId, client.unregisteredInstanceId);
    }

    @Test
    void retriesBootstrapRegistrationUntilDependenciesBecomeAvailable() throws InterruptedException {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water",
                "catalog-service",
                "1.0.0",
                "",
                "http",
                "/water/catalog",
                "",
                "8381",
                "127.0.0.1"
        );
        FixedGlobalOptions globalOptions = new FixedGlobalOptions("http://127.0.0.1:8181/water", "127.0.0.1");
        InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
        support.register(registry, options);
        Thread.sleep(250L);
        Assertions.assertNull(client.registeredInfo);

        registry.register(ServiceDiscoveryGlobalOptions.class, globalOptions);
        registry.register(ServiceDiscoveryRegistryClientInternal.class, client);
        registry.register(ServiceLivenessClient.class, livenessClient);

        long timeoutAt = System.currentTimeMillis() + 2500L;
        while (System.currentTimeMillis() < timeoutAt && client.registeredInfo == null) {
            Thread.sleep(100L);
        }

        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertNotNull(livenessClient.lastRegistration);
        Assertions.assertEquals("catalog-service", client.registeredInfo.getServiceId());
        Assertions.assertEquals("127.0.0.1", client.registeredInfo.getServiceHost());
        support.deregister(null);
    }

    @Test
    void validateEndpointReachabilityTreatsHttp500AsNotReady() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturning(500));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "catalog-service", "catalog-1",
                        "/water/catalog", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("NOT_READY_OR_UNREACHABLE", outcome.name());
    }

    @Test
    void validateEndpointReachabilityAcceptsUnauthorizedEndpoints() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturning(401));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "catalog-service", "catalog-1",
                        "/water/catalog", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    private static void setEndpointCheckClient(ServiceRegistrationLifecycleSupport support, HttpClient httpClient) throws Exception {
        Field field = ServiceRegistrationLifecycleSupport.class.getDeclaredField("endpointCheckClient");
        field.setAccessible(true);
        field.set(support, httpClient);
    }

    private static Enum<?> invokeEndpointValidation(ServiceRegistrationLifecycleSupport support,
                                                    DiscoverableServiceInfoImpl serviceInfo) throws Exception {
        Method method = ServiceRegistrationLifecycleSupport.class
                .getDeclaredMethod("validateEndpointReachability", DiscoverableServiceInfoImpl.class);
        method.setAccessible(true);
        return (Enum<?>) method.invoke(support, serviceInfo);
    }

    @SuppressWarnings("unchecked")
    private static HttpClient mockHttpClientReturning(int statusCode) throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse<Void> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(httpClient.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return httpClient;
    }

    private static final class TestServiceRegistrationLifecycleSupport extends ServiceRegistrationLifecycleSupport {
        void register(ServiceDiscoveryRegistryClientInternal client,
                      ServiceRegistrationOptions options,
                      ServiceLivenessClient livenessClient) {
            doRegister(client, options, null, null, livenessClient);
        }

        void register(ComponentRegistry componentRegistry, ServiceRegistrationOptions options) {
            bootstrapRegister(componentRegistry, options);
        }

        void deregister(ServiceDiscoveryRegistryClientInternal client) {
            doDeregister(client);
        }

        String currentRegisteredServiceName() {
            return registeredServiceName;
        }

        String currentRegisteredInstanceId() {
            return registeredInstanceId;
        }
    }

    private static final class RecordingRegistryClient implements ServiceDiscoveryRegistryClientInternal {
        private String setupRemoteUrl;
        private String setupPort;
        private DiscoverableServiceInfoImpl registeredInfo;
        private String unregisteredServiceName;
        private String unregisteredInstanceId;

        @Override
        public void registerService(DiscoverableServiceInfo registration) {
            this.registeredInfo = (DiscoverableServiceInfoImpl) registration;
        }

        @Override
        public void unregisterService(String serviceName, String instanceId) {
            this.unregisteredServiceName = serviceName;
            this.unregisteredInstanceId = instanceId;
            if (registeredInfo != null && instanceId.equals(registeredInfo.getServiceInstanceId())) {
                registeredInfo = null;
            }
        }

        @Override
        public DiscoverableServiceInfo getServiceInfo(String id) {
            return registeredInfo;
        }

        @Override
        public void setup(String remoteUrl, String port) {
            this.setupRemoteUrl = remoteUrl;
            this.setupPort = port;
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

    private static final class FixedRegistrationOptions implements ServiceRegistrationOptions {
        private final String discoveryUrl;
        private final String serviceName;
        private final String serviceVersion;
        private final String instanceId;
        private final String protocol;
        private final String root;
        private final String advertisedEndpoint;
        private final String servicePort;
        private final String serviceHost;

        private FixedRegistrationOptions(String discoveryUrl,
                                         String serviceName,
                                         String serviceVersion,
                                         String instanceId,
                                         String protocol,
                                         String root,
                                         String advertisedEndpoint,
                                         String servicePort,
                                         String serviceHost) {
            this.discoveryUrl = discoveryUrl;
            this.serviceName = serviceName;
            this.serviceVersion = serviceVersion;
            this.instanceId = instanceId;
            this.protocol = protocol;
            this.root = root;
            this.advertisedEndpoint = advertisedEndpoint;
            this.servicePort = servicePort;
            this.serviceHost = serviceHost;
        }

        @Override
        public String getDiscoveryUrl() {
            return discoveryUrl;
        }

        @Override
        public String getServiceName() {
            return serviceName;
        }

        @Override
        public String getServiceVersion() {
            return serviceVersion;
        }

        @Override
        public String getInstanceId() {
            return instanceId;
        }

        @Override
        public String getProtocol() {
            return protocol;
        }

        @Override
        public String getRoot() {
            return root;
        }

        @Override
        public String getAdvertisedEndpoint() {
            return advertisedEndpoint;
        }

        @Override
        public String getServicePort() {
            return servicePort;
        }

        @Override
        public String getServiceHost() {
            return serviceHost;
        }
    }
}
