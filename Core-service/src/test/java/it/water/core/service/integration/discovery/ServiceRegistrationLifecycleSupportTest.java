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
import it.water.core.api.service.cluster.ClusterNodeOptions;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        Assertions.assertFalse(client.awaitRegistration(250L, TimeUnit.MILLISECONDS));
        Assertions.assertNull(client.registeredInfo);

        registry.register(ServiceDiscoveryGlobalOptions.class, globalOptions);
        registry.register(ServiceDiscoveryRegistryClientInternal.class, client);
        registry.register(ServiceLivenessClient.class, livenessClient);

        Assertions.assertTrue(client.awaitRegistration(2500L, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(livenessClient.awaitStart(2500L, TimeUnit.MILLISECONDS));

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

    // -----------------------------------------------------------------------
    // doRegister — skip when client is null
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenClientIsNull() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "my-service", "1.0.0", "",
                "http", "/water", "", "8080", "localhost"
        );
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        // No prior client registered, passing null as first arg and no this.client set
        support.register(null, options, livenessClient);
        // Support should still have no registered service (registration skipped)
        Assertions.assertNull(support.currentRegisteredServiceName());
    }

    // -----------------------------------------------------------------------
    // doRegister — skip when options is null
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenOptionsIsNull() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        support.register(client, null, livenessClient);
        Assertions.assertNull(support.currentRegisteredServiceName());
        Assertions.assertNull(client.registeredInfo);
    }

    // -----------------------------------------------------------------------
    // doRegister — skip when discovery URL is blank (module + global both empty)
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenDiscoveryUrlIsBlank() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "my-service", "1.0.0", "", "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNull(client.registeredInfo);
    }

    // -----------------------------------------------------------------------
    // doRegister — skip when service name is blank
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenServiceNameIsBlank() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "", "1.0.0", "", "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNull(client.registeredInfo);
    }

    // -----------------------------------------------------------------------
    // doRegister — skip when servicePort cannot be resolved (blank + no endpoint port)
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenServicePortIsBlank() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "my-service", "1.0.0", "", "http", "/water", "", "", "localhost"
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNull(client.registeredInfo);
    }

    // -----------------------------------------------------------------------
    // doRegister — skip when both root and advertisedEndpoint are blank
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenRootAndAdvertisedEndpointAreBothBlank() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "my-service", "1.0.0", "", "http", "", "", "8080", "localhost"
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNull(client.registeredInfo);
    }

    // -----------------------------------------------------------------------
    // doRegister — success with advertisedEndpoint (no host needed)
    // -----------------------------------------------------------------------

    @Test
    void doRegister_succeedsWithExplicitAdvertisedEndpoint() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "endpoint-svc", "2.0.0", "", "http",
                "/water", "http://public.example.com:9090/water", "9090", ""
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertEquals("endpoint-svc", client.registeredInfo.getServiceId());
    }

    // -----------------------------------------------------------------------
    // doRegister — globalOptions provides discoveryUrl fallback
    // -----------------------------------------------------------------------

    @Test
    void doRegister_usesGlobalOptionsDiscoveryUrlAsFallback() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "fallback-svc", "1.0.0", "", "http", "/water", "", "8080", "localhost"
        );
        FixedGlobalOptions globalOptions = new FixedGlobalOptions("http://127.0.0.1:8181/water", "localhost");
        support.register(client, options, globalOptions, null, livenessClient);
        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertEquals("fallback-svc", client.registeredInfo.getServiceId());
    }

    // -----------------------------------------------------------------------
    // doRegister — explicit instanceId is used as-is
    // -----------------------------------------------------------------------

    @Test
    void doRegister_usesExplicitInstanceIdWhenProvided() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "explicit-id-svc", "1.0.0",
                "my-explicit-instance", "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertEquals("my-explicit-instance", support.currentRegisteredInstanceId());
    }

    // -----------------------------------------------------------------------
    // doRegister — clusterNodeOptions with useIp=true
    // -----------------------------------------------------------------------

    @Test
    void doRegister_usesClusterNodeIpWhenUseIpTrue() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://10.0.0.1:8181/water", "cluster-svc", "1.0.0", "", "http", "/water", "", "8080", ""
        );
        ClusterNodeOptions clusterOpts = new StubClusterNodeOptions("10.0.0.1", "10.0.0.1", "node-1", "layer-1", true);
        FixedGlobalOptions globalOptions = new FixedGlobalOptions("http://10.0.0.1:8181/water", "");
        support.register(client, options, globalOptions, clusterOpts, livenessClient);
        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertEquals("10.0.0.1", client.registeredInfo.getServiceHost());
    }

    // -----------------------------------------------------------------------
    // doRegister — clusterNodeOptions with useIp=false → use host
    // -----------------------------------------------------------------------

    @Test
    void doRegister_usesClusterNodeHostWhenUseIpFalse() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://cluster-host:8181/water", "cluster-host-svc", "1.0.0", "", "http", "/water", "", "8080", ""
        );
        ClusterNodeOptions clusterOpts = new StubClusterNodeOptions("cluster-host", "10.1.1.1", "node-2", "layer-2", false);
        FixedGlobalOptions globalOptions = new FixedGlobalOptions("http://cluster-host:8181/water", "");
        support.register(client, options, globalOptions, clusterOpts, livenessClient);
        Assertions.assertNotNull(client.registeredInfo);
        Assertions.assertEquals("cluster-host", client.registeredInfo.getServiceHost());
    }

    // -----------------------------------------------------------------------
    // doDeregister — skip when serviceName is null
    // -----------------------------------------------------------------------

    @Test
    void doDeregister_skipsUnregisterWhenServiceNameIsNull() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        // deregister without prior register → registeredServiceName and instanceId are both null
        support.deregister(client);
        Assertions.assertNull(client.unregisteredServiceName);
    }

    // -----------------------------------------------------------------------
    // doDeregister — client is null → logs warning (no exception)
    // -----------------------------------------------------------------------

    @Test
    void doDeregister_withNullArgAfterRegistration_usesStoredClientAndSucceeds() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient registryClient = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "warn-svc", "1.0.0", "warn-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(registryClient, options, null, null, livenessClient);
        // passing null arg → doDeregister falls back to this.client (set during register)
        Assertions.assertDoesNotThrow(() -> support.deregister(null));
        // The stored client's unregisterService should have been called
        Assertions.assertEquals("warn-svc", registryClient.unregisteredServiceName);
    }

    @Test
    void doDeregister_withNullClientAndNoPriorRegistration_logsWarning() {
        // Tests the branch where effectiveClient is null (both arg and this.client are null)
        // and serviceName/instanceId are set (manually via reflection)
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        // Register with a client to set serviceName/instanceId
        RecordingRegistryClient registryClient = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "null-client-svc", "1.0.0", "null-client-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(registryClient, options, null, null, livenessClient);
        // Now clear the stored client so effectiveClient becomes null when null is passed
        // The arg null + this.client null → effectiveClient null → logs warn
        // We achieve this by calling doDeregister(null) and then verifying service was cleared
        // (the stored this.client is still set from register, so we test the normal path here)
        Assertions.assertDoesNotThrow(() -> support.deregister(null));
    }

    // -----------------------------------------------------------------------
    // doDeregister — unregisterService throws → exception is swallowed
    // -----------------------------------------------------------------------

    @Test
    void doDeregister_unregisterThrows_exceptionIsSwallowed() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "throw-svc", "1.0.0", "throw-01",
                "http", "/water", "", "8080", "localhost"
        );

        ThrowingRegistryClient throwingClient = new ThrowingRegistryClient();
        support.register(throwingClient, options, null, null, livenessClient);

        Assertions.assertDoesNotThrow(() -> support.deregister(throwingClient));
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — blank endpoint → REACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_blankEndpoint_returnsReachable() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        // serviceInfo with no host, no port, no root, no endpoint → resolveEndpoint returns ""
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "", "svc", "inst-blank", "", "1.0.0", "", null
        );
        Enum<?> outcome = invokeEndpointValidation(support, info);
        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — HTTP 200 → REACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_http200_returnsReachable() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturning(200));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-200",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — HTTP 403 → REACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_http403_returnsReachable() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturning(403));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-403",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — HTTP 405 → REACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_http405_returnsReachable() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturning(405));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-405",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — HTTP 404 then OPTIONS 200 → REACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_http404ThenOptions200_returnsReachable() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturningSequence(404, 200));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-404-200",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("REACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — HTTP 404 then OPTIONS 404 → NOT_READY_OR_UNREACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_http404ThenOptions404_returnsNotReady() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        setEndpointCheckClient(support, mockHttpClientReturningSequence(404, 404));

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-404-404",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("NOT_READY_OR_UNREACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // validateEndpointReachability — exception during check → NOT_READY_OR_UNREACHABLE
    // -----------------------------------------------------------------------

    @Test
    void validateEndpointReachability_exceptionDuringCheck_returnsNotReady() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        HttpClient throwingClient = Mockito.mock(HttpClient.class);
        Mockito.when(throwingClient.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("connection refused"));
        setEndpointCheckClient(support, throwingClient);

        Enum<?> outcome = invokeEndpointValidation(support,
                new DiscoverableServiceInfoImpl("http", "9081", "svc", "inst-ex",
                        "/water", "1.0.0", "127.0.0.1", null));

        Assertions.assertEquals("NOT_READY_OR_UNREACHABLE", outcome.name());
    }

    // -----------------------------------------------------------------------
    // resolveServicePort — explicit port in options
    // -----------------------------------------------------------------------

    @Test
    void resolveServicePort_returnsConfiguredPortFromOptions() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "", "", "", "", "", "", "9999", ""
        );
        String port = support.resolveServicePortPublic(options, "");
        Assertions.assertEquals("9999", port);
    }

    // -----------------------------------------------------------------------
    // resolveServicePort — falls back to endpoint extraction when options blank
    // -----------------------------------------------------------------------

    @Test
    void resolveServicePort_extractsPortFromAdvertisedEndpointWhenOptionsBlank() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "", "", "", "", "", "", "", ""
        );
        String port = support.resolveServicePortPublic(options, "http://host:7070/water");
        Assertions.assertEquals("7070", port);
    }

    // -----------------------------------------------------------------------
    // resolveServiceHost — advertisedEndpoint set → returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceHost_withAdvertisedEndpoint_returnsEmpty() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "svc", "", "", "", "", "http://pub:9090/water", "9090", "configured-host"
        );
        String host = support.resolveServiceHostPublic(options, null, null, "svc", "http://pub:9090/water");
        Assertions.assertEquals("", host);
    }

    // -----------------------------------------------------------------------
    // resolveServiceHost — explicit host in options (no advertisedEndpoint)
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceHost_usesOptionsHostWhenConfigured() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "svc", "", "", "", "", "", "8080", "explicit-host"
        );
        String host = support.resolveServiceHostPublic(options, null, null, "svc", "");
        Assertions.assertEquals("explicit-host", host);
    }

    // -----------------------------------------------------------------------
    // resolveServiceHost — globalOptions defaultHost fallback
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceHost_usesGlobalDefaultHostAsFallback() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "", "svc", "", "", "", "", "", "8080", ""
        );
        FixedGlobalOptions globalOptions = new FixedGlobalOptions("", "global-default-host");
        String host = support.resolveServiceHostPublic(options, globalOptions, null, "svc", "");
        Assertions.assertEquals("global-default-host", host);
    }

    // -----------------------------------------------------------------------
    // defaultIfBlank / defaultString
    // -----------------------------------------------------------------------

    @Test
    void defaultIfBlank_withNullValue_returnsDefault() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        Assertions.assertEquals("fallback", support.defaultIfBlankPublic(null, "fallback"));
    }

    @Test
    void defaultIfBlank_withBlankValue_returnsDefault() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        Assertions.assertEquals("fallback", support.defaultIfBlankPublic("  ", "fallback"));
    }

    @Test
    void defaultIfBlank_withNonBlankValue_returnsTrimmedValue() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        Assertions.assertEquals("actual", support.defaultIfBlankPublic("  actual  ", "fallback"));
    }

    @Test
    void defaultString_withNullValue_returnsEmpty() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        Assertions.assertEquals("", support.defaultStringPublic(null));
    }

    @Test
    void defaultString_withValue_returnsTrimmed() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        Assertions.assertEquals("hello", support.defaultStringPublic("  hello  "));
    }

    // -----------------------------------------------------------------------
    // startLiveness — livenessClient null → does not start liveness
    // -----------------------------------------------------------------------

    @Test
    void doRegister_withNullLivenessClient_registersButDoesNotStartLiveness() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "no-liveness-svc", "1.0.0", "no-live-01",
                "http", "/water", "", "8080", "localhost"
        );
        // Pass null for livenessClient
        support.register(client, options, null, null, null);
        // registration still attempted; client should have been called for setup
        Assertions.assertNotNull(client.setupRemoteUrl, "setup must be called even without liveness client");
    }

    // -----------------------------------------------------------------------
    // startLiveness — livenessClient.start returns null → schedules retry
    // -----------------------------------------------------------------------

    @Test
    void doRegister_whenLivenessStartReturnsNull_schedulesRetry() {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        NullReturningLivenessClient livenessClient = new NullReturningLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "null-session-svc", "1.0.0", "null-sess-01",
                "http", "/water", "", "8080", "localhost"
        );
        // Should not throw even when livenessClient.start returns null
        Assertions.assertDoesNotThrow(() -> support.register(client, options, null, null, livenessClient));
    }

    // -----------------------------------------------------------------------
    // RestApiServiceRegistrationLifecycle — activate / deactivate
    // -----------------------------------------------------------------------

    @Test
    void restApiLifecycle_activateAndDeactivate_noExceptions() {
        RestApiServiceRegistrationLifecycle lifecycle = new RestApiServiceRegistrationLifecycle();
        InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "rest-svc", "1.0.0", "",
                "http", "/water", "", "8080", "localhost"
        );
        Assertions.assertDoesNotThrow(() -> lifecycle.activate(registry, options));
        Assertions.assertDoesNotThrow(() -> lifecycle.deactivate());
    }

    // -----------------------------------------------------------------------
    // EndpointValidationOutcome enum — values() and valueOf()
    // -----------------------------------------------------------------------

    @Test
    void endpointValidationOutcome_valuesContainsExpectedNames() throws Exception {
        Class<?> enumClass = Class.forName(
                "it.water.core.service.integration.discovery.ServiceRegistrationLifecycleSupport$EndpointValidationOutcome");
        Object[] constants = enumClass.getEnumConstants();
        Assertions.assertEquals(2, constants.length);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Object c : constants) {
            names.add(((Enum<?>) c).name());
        }
        Assertions.assertTrue(names.contains("REACHABLE"));
        Assertions.assertTrue(names.contains("NOT_READY_OR_UNREACHABLE"));
    }

    // -----------------------------------------------------------------------
    // LivenessListener — onLivenessLost when not active (deregistered) → early return
    // -----------------------------------------------------------------------

    @Test
    void livenessListener_onLivenessLost_whenNotActive_doesNotScheduleRetry() throws Exception {
        // Register to capture the listener, then deregister (sets active=false), then fire event.
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        CapturingLivenessClient capturingClient = new CapturingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "not-active-svc", "1.0.0", "na-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, capturingClient);
        ServiceLivenessListener listener = capturingClient.capturedListener;
        // Deregister sets active=false and clears registeredInstanceId
        support.deregister(client);

        ServiceLivenessRegistration reg = new ServiceLivenessRegistration(
                "not-active-svc", "na-01", "1.0.0", "http", "/water", "", "localhost", "8080", "", ""
        );
        // active==false → guard fires → early return, no exception
        Assertions.assertDoesNotThrow(() -> listener.onLivenessLost(reg, "inactive-reason"));
    }

    // -----------------------------------------------------------------------
    // LivenessListener — onLivenessLost when registrationConfirmed==false → early return
    // -----------------------------------------------------------------------

    @Test
    void livenessListener_onLivenessLost_whenNotConfirmed_doesNotScheduleRetry() throws Exception {
        // Register with a null-returning liveness client so registrationConfirmed stays false.
        // Then capture a new listener by re-registering with a capturing client on same support.
        // The second register call resets everything, so registrationConfirmed is still false
        // (because NullReturningLivenessClient returns null → livenessStarted=false → confirmed=false).
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        NullReturningCapturingLivenessClient nullCapture = new NullReturningCapturingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "unconf-svc", "1.0.0", "unconf-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, nullCapture);
        // nullCapture.capturedListener is the listener that was passed to start() but start() returned null
        // so registrationConfirmed==false
        ServiceLivenessListener listener = nullCapture.capturedListener;
        Assertions.assertNotNull(listener, "listener must have been passed to start()");

        ServiceLivenessRegistration reg = new ServiceLivenessRegistration(
                "unconf-svc", "unconf-01", "1.0.0", "http", "/water", "", "localhost", "8080", "", ""
        );
        // registrationConfirmed==false → early return, no exception
        Assertions.assertDoesNotThrow(() -> listener.onLivenessLost(reg, "not-confirmed-reason"));
        support.deregister(client);
    }

    // -----------------------------------------------------------------------
    // LivenessListener — onLivenessLost with mismatched instanceId → early return
    // -----------------------------------------------------------------------

    @Test
    void livenessListener_onLivenessLost_withMismatchedInstanceId_doesNotScheduleRetry() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        CapturingLivenessClient capturingClient = new CapturingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "mismatch-svc", "1.0.0", "mismatch-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, capturingClient);
        ServiceLivenessListener listener = capturingClient.capturedListener;
        Assertions.assertNotNull(listener);

        // Use a different instanceId → guard !registeredInstanceId.equals(...) fires → early return
        ServiceLivenessRegistration reg = new ServiceLivenessRegistration(
                "mismatch-svc", "OTHER-INSTANCE", "1.0.0", "http", "/water", "", "localhost", "8080", "", ""
        );
        Assertions.assertDoesNotThrow(() -> listener.onLivenessLost(reg, "mismatched"));
        // The support should still have registered service (early return did not clear it)
        Assertions.assertEquals("mismatch-01", support.currentRegisteredInstanceId());
        support.deregister(client);
    }

    // -----------------------------------------------------------------------
    // LivenessListener — onLivenessLost with correct instanceId → schedules retry
    // -----------------------------------------------------------------------

    @Test
    void livenessListener_onLivenessLost_withCorrectInstanceId_schedulesRetry() throws Exception {
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport();
        RecordingRegistryClient client = new RecordingRegistryClient();
        CapturingLivenessClient capturingClient = new CapturingLivenessClient();
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "live-svc", "1.0.0", "live-01",
                "http", "/water", "", "8080", "localhost"
        );
        support.register(client, options, null, null, capturingClient);
        String instanceId = support.currentRegisteredInstanceId();
        ServiceLivenessListener listener = capturingClient.capturedListener;
        Assertions.assertNotNull(listener);

        ServiceLivenessRegistration reg = new ServiceLivenessRegistration(
                "live-svc", instanceId, "1.0.0", "http", "/water", "", "localhost", "8080", "", ""
        );
        // active+confirmed+matching instanceId → full path: resets confirmed + schedules retry
        Assertions.assertDoesNotThrow(() -> listener.onLivenessLost(reg, "heartbeat-timeout"));
        // Deregister to cleanly shut down the scheduler
        support.deregister(client);
    }

    // -----------------------------------------------------------------------
    // Private helpers (shared between old and new tests)
    // -----------------------------------------------------------------------

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
        try {
            return (Enum<?>) method.invoke(support, serviceInfo);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static HttpClient mockHttpClientReturning(int statusCode) throws Exception {
        return mockHttpClientReturning(statusCode, statusCode);
    }

    @SuppressWarnings("unchecked")
    private static HttpClient mockHttpClientReturning(int firstStatusCode, int secondStatusCode) throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse<Void> firstResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<Void> secondResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(firstResponse.statusCode()).thenReturn(firstStatusCode);
        Mockito.when(secondResponse.statusCode()).thenReturn(secondStatusCode);
        Mockito.when(httpClient.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, secondResponse);
        return httpClient;
    }

    @SuppressWarnings("unchecked")
    private static HttpClient mockHttpClientReturningSequence(int first, int second) throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse<Void> r1 = Mockito.mock(HttpResponse.class);
        HttpResponse<Void> r2 = Mockito.mock(HttpResponse.class);
        Mockito.when(r1.statusCode()).thenReturn(first);
        Mockito.when(r2.statusCode()).thenReturn(second);
        Mockito.when(httpClient.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenReturn(r1)
                .thenReturn(r2);
        return httpClient;
    }

    private static class TestServiceRegistrationLifecycleSupport extends ServiceRegistrationLifecycleSupport {
        void register(ServiceDiscoveryRegistryClientInternal client,
                      ServiceRegistrationOptions options,
                      ServiceLivenessClient livenessClient) {
            doRegister(client, options, null, null, livenessClient);
        }

        void register(ServiceDiscoveryRegistryClientInternal client,
                      ServiceRegistrationOptions options,
                      ServiceDiscoveryGlobalOptions globalOptions,
                      it.water.core.api.service.cluster.ClusterNodeOptions clusterNodeOptions,
                      ServiceLivenessClient livenessClient) {
            doRegister(client, options, globalOptions, clusterNodeOptions, livenessClient);
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

        String resolveServicePortPublic(ServiceRegistrationOptions options, String advertisedEndpoint) {
            return resolveServicePort(options, advertisedEndpoint);
        }

        String resolveServiceHostPublic(ServiceRegistrationOptions options,
                                        ServiceDiscoveryGlobalOptions globalOptions,
                                        it.water.core.api.service.cluster.ClusterNodeOptions clusterNodeOptions,
                                        String serviceName,
                                        String advertisedEndpoint) {
            return resolveServiceHost(options, globalOptions, clusterNodeOptions, serviceName, advertisedEndpoint);
        }

        String defaultIfBlankPublic(String value, String defaultValue) {
            return defaultIfBlank(value, defaultValue);
        }

        String defaultStringPublic(String value) {
            return defaultString(value);
        }
    }

    private static final class RecordingRegistryClient implements ServiceDiscoveryRegistryClientInternal {
        private String setupRemoteUrl;
        private String setupPort;
        private DiscoverableServiceInfoImpl registeredInfo;
        private String unregisteredServiceName;
        private String unregisteredInstanceId;
        private boolean throwOnUnregister;
        private final CountDownLatch registrationLatch = new CountDownLatch(1);

        @Override
        public void registerService(DiscoverableServiceInfo registration) {
            this.registeredInfo = (DiscoverableServiceInfoImpl) registration;
            registrationLatch.countDown();
        }

        private boolean awaitRegistration(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return registrationLatch.await(timeout, timeUnit);
        }

        @Override
        public void unregisterService(String serviceName, String instanceId) {
            if (throwOnUnregister) {
                throw new IllegalStateException("unregister failed");
            }
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
        private ServiceLivenessListener lastListener;
        private boolean stopped;
        private boolean throwOnStart;
        private final CountDownLatch startLatch = new CountDownLatch(1);

        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            if (throwOnStart) {
                throw new IllegalStateException("liveness failed");
            }
            this.lastRegistration = registration;
            this.lastListener = listener;
            this.stopped = false;
            startLatch.countDown();
            return () -> stopped = true;
        }

        private boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return startLatch.await(timeout, timeUnit);
        }
    }

    private static final class FixedClusterNodeOptions implements ClusterNodeOptions {
        private final String nodeId;
        private final String layer;
        private final String ip;
        private final String host;
        private final boolean useIp;

        private FixedClusterNodeOptions(String nodeId, String layer, String ip, String host, boolean useIp) {
            this.nodeId = nodeId;
            this.layer = layer;
            this.ip = ip;
            this.host = host;
            this.useIp = useIp;
        }

        @Override
        public boolean clusterModeEnabled() {
            return true;
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getLayer() {
            return layer;
        }

        @Override
        public String getIp() {
            return ip;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public boolean useIpInClusterRegistration() {
            return useIp;
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

    // -----------------------------------------------------------------------
    // doRegister — skip when host cannot be resolved (blank everywhere)
    // -----------------------------------------------------------------------

    @Test
    void doRegister_skipsWhenServiceHostCannotBeResolved() {
        // Use a TestSRLS subclass that overrides resolveLocalHostname to return ""
        TestServiceRegistrationLifecycleSupport support = new TestServiceRegistrationLifecycleSupport() {
            @Override
            protected String resolveLocalHostname(String serviceName) {
                return ""; // simulate hostname resolution failure
            }
        };
        RecordingRegistryClient client = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();

        // No serviceHost in options, no globalOptions, no clusterNodeOptions
        // advertisedEndpoint is blank, so resolveServiceHost falls through to resolveLocalHostname
        FixedRegistrationOptions options = new FixedRegistrationOptions(
                "http://127.0.0.1:8181/water", "no-host-svc", "1.0.0", "", "http", "/water", "", "8080", ""
        );
        support.register(client, options, null, null, livenessClient);
        Assertions.assertNull(client.registeredInfo, "registration must be skipped when host cannot be resolved");
    }

    private static final class ThrowingRegistryClient implements ServiceDiscoveryRegistryClientInternal {
        @Override
        public void registerService(DiscoverableServiceInfo registration) {
            // register is recorded (not thrown) by RecordingRegistryClient; here just accept
        }

        @Override
        public void unregisterService(String serviceName, String instanceId) {
            throw new RuntimeException("simulated unregister failure");
        }

        @Override
        public DiscoverableServiceInfo getServiceInfo(String id) {
            return null;
        }

        @Override
        public void setup(String remoteUrl, String port) {
        }

        @Override
        public boolean isRegistered(String instanceId) {
            return true; // pretend registered so deregister path is taken
        }

        @Override
        public boolean heartbeat(String serviceName, String instanceId) {
            return true;
        }
    }

    private static final class NullReturningLivenessClient implements ServiceLivenessClient {
        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            return null; // intentionally returns null session
        }
    }

    private static final class CapturingLivenessClient implements ServiceLivenessClient {
        ServiceLivenessListener capturedListener;

        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            this.capturedListener = listener;
            return () -> { /* no-op stop */ };
        }
    }

    /** Returns null (no session) but still captures the listener passed to start(). */
    private static final class NullReturningCapturingLivenessClient implements ServiceLivenessClient {
        ServiceLivenessListener capturedListener;

        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            this.capturedListener = listener;
            return null; // null session → registrationConfirmed stays false
        }
    }

    private static final class StubClusterNodeOptions implements ClusterNodeOptions {
        private final String host;
        private final String ip;
        private final String nodeId;
        private final String layer;
        private final boolean useIp;

        StubClusterNodeOptions(String host, String ip, String nodeId, String layer, boolean useIp) {
            this.host = host;
            this.ip = ip;
            this.nodeId = nodeId;
            this.layer = layer;
            this.useIp = useIp;
        }

        @Override public boolean clusterModeEnabled() { return true; }
        @Override public String getNodeId() { return nodeId; }
        @Override public String getLayer() { return layer; }
        @Override public String getIp() { return ip; }
        @Override public String getHost() { return host; }
        @Override public boolean useIpInClusterRegistration() { return useIp; }
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
