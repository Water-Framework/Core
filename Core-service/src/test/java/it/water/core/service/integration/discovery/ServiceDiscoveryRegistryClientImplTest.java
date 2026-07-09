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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

class ServiceDiscoveryRegistryClientImplTest {

    private HttpServer server;
    private int serverPort;
    private final AtomicReference<String> postBody = new AtomicReference<>();
    private final AtomicReference<String> deletePath = new AtomicReference<>();
    private final AtomicReference<String> heartbeatPath = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/water/internal/serviceregistration", this::handleInternalDiscoveryRequest);
        server.createContext("/water/serviceregistration", this::handlePublicDiscoveryRequest);
        server.start();
        serverPort = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registerGetAndUnregisterUseExpectedPayload() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water/", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "catalog-01", "/catalog", "2.1.0",
                "public-host", null
        );

        client.registerService(info);

        Assertions.assertTrue(client.isRegistered("catalog-01"));
        Assertions.assertNotNull(postBody.get());
        Assertions.assertTrue(postBody.get().contains("\"serviceName\":\"catalog-service\""));
        Assertions.assertTrue(postBody.get().contains("\"serviceVersion\":\"2.1.0\""));
        Assertions.assertTrue(postBody.get().contains("\"instanceId\":\"catalog-01\""));
        Assertions.assertTrue(postBody.get().contains("http://public-host:9191/catalog"));

        DiscoverableServiceInfoImpl remoteInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("42");
        Assertions.assertNotNull(remoteInfo);
        Assertions.assertEquals("catalog-service", remoteInfo.getServiceId());
        Assertions.assertEquals("catalog-01", remoteInfo.getServiceInstanceId());
        Assertions.assertEquals("2.1.0", remoteInfo.getServiceVersion());
        Assertions.assertEquals("remote-host", remoteInfo.getServiceHost());
        Assertions.assertEquals("http://remote-host:8181/water", remoteInfo.getServiceEndpoint());
        Assertions.assertEquals("8181", remoteInfo.getServicePort());
        Assertions.assertEquals("/water", remoteInfo.getServiceRoot());

        client.unregisterService("catalog-service", "catalog-01");

        Assertions.assertEquals("/water/internal/serviceregistration/catalog-service/catalog-01", deletePath.get());
        Assertions.assertFalse(client.isRegistered("catalog-01"));
    }

    @Test
    void registerUsesExplicitAdvertisedEndpointWhenProvided() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "catalog-02", "/catalog", "2.1.0",
                null, "https://public.example.com/catalog"
        );

        client.registerService(info);

        Assertions.assertNotNull(postBody.get());
        Assertions.assertTrue(postBody.get().contains("\"endpoint\":\"https://public.example.com/catalog\""));
    }

    @Test
    void registerShouldLeaveInstanceUnregisteredWhenServerRejectsRequest() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setGlobalOptions(shortRetryOptions());
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "reject-service", "reject-01", "/reject", "1.0.0",
                "public-host", null
        );

        client.registerService(info);

        Assertions.assertFalse(client.isRegistered("reject-01"));
    }

    @Test
    void registerShouldRejectUnsupportedServiceInfoImplementation() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfo info = new DiscoverableServiceInfo() {
            @Override
            public String getServiceProtocol() {
                return "http";
            }

            @Override
            public String getServicePort() {
                return "9191";
            }

            @Override
            public String getServiceId() {
                return "catalog-service";
            }

            @Override
            public String getServiceInstanceId() {
                return "catalog-unsupported";
            }

            @Override
            public String getServiceRoot() {
                return "/catalog";
            }
        };

        Assertions.assertThrows(IllegalArgumentException.class, () -> client.registerService(info));
    }

    @Test
    void unregisterShouldKeepRegistrationWhenServerReturnsError() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "error-01", "/catalog", "2.1.0",
                "public-host", null
        );
        client.registerService(info);

        client.unregisterService("error-service", "error-01");

        Assertions.assertTrue(client.isRegistered("error-01"));
    }

    @Test
    void heartbeatReturnsTrueOn204AndFalseOn404() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "catalog-01", "/catalog", "2.1.0",
                "public-host", null
        );
        client.registerService(info);

        Assertions.assertTrue(client.heartbeat("catalog-service", "catalog-01"));
        Assertions.assertEquals("/water/internal/serviceregistration/heartbeat/catalog-service/catalog-01", heartbeatPath.get());
        Assertions.assertFalse(client.heartbeat("missing-service", "missing-instance"));
    }

    @Test
    void heartbeatShouldReturnFalseWhenServerReturnsError() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        Assertions.assertFalse(client.heartbeat("error-service", "error-instance"));
    }

    @Test
    void registerFailsWhenEndpointCannotBeResolved() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "", "catalog-service", "catalog-03", "", "2.1.0",
                "", ""
        );

        Assertions.assertThrows(IllegalStateException.class, () -> client.registerService(info));
    }

    @Test
    void getServiceInfoShouldUseWaterRootWhenDiscoveryUrlHasNoRoot() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort, "9191");

        DiscoverableServiceInfoImpl remoteInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("42");

        Assertions.assertNotNull(remoteInfo);
        Assertions.assertEquals("catalog-service", remoteInfo.getServiceId());
    }

    @Test
    void getServiceInfoUsesStandardPortDefaults() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl httpInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("80");
        Assertions.assertNotNull(httpInfo);
        Assertions.assertEquals("remote-host", httpInfo.getServiceHost());
        Assertions.assertEquals("http://remote-host/water", httpInfo.getServiceEndpoint());
        Assertions.assertEquals("80", httpInfo.getServicePort());
        Assertions.assertEquals("/water", httpInfo.getServiceRoot());

        DiscoverableServiceInfoImpl httpsInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("443");
        Assertions.assertNotNull(httpsInfo);
        Assertions.assertEquals("secure-host", httpsInfo.getServiceHost());
        Assertions.assertEquals("https://secure-host/secure", httpsInfo.getServiceEndpoint());
        Assertions.assertEquals("443", httpsInfo.getServicePort());
        Assertions.assertEquals("/secure", httpsInfo.getServiceRoot());
    }

    @Test
    void resolveEndpoint_throwsWhenRemoteUrlNotConfigured() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // remoteUrl is null → resolveEndpoint must throw IllegalStateException
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9090", "svc", "inst-x", "/water", "1.0.0",
                "localhost", null
        );
        Assertions.assertThrows(IllegalStateException.class, () -> client.registerService(info));
    }

    @Test
    void resolveEndpoint_withoutWaterSuffix_appendsWaterContext() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // URL does NOT end with "/water" → code must append /water before path
        client.setup("http://localhost:" + serverPort, "8181");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "8181", "no-water-svc", "nw-01", "/water", "1.0.0",
                "localhost", null
        );
        // The handler is already registered at /water/... so the call goes through
        client.registerService(info);
        Assertions.assertTrue(client.isRegistered("nw-01"));
    }

    @Test
    void buildRegistrationJson_throwsWhenInfoIsNotDiscoverableServiceInfoImpl() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // Create a non-DiscoverableServiceInfoImpl implementation
        it.water.core.api.service.integration.discovery.DiscoverableServiceInfo wrongType =
                new it.water.core.api.service.integration.discovery.DiscoverableServiceInfo() {
                    @Override public String getServiceProtocol() { return "http"; }
                    @Override public String getServicePort() { return "9090"; }
                    @Override public String getServiceId() { return "wrong-type-svc"; }
                    @Override public String getServiceInstanceId() { return "wt-01"; }
                    @Override public String getServiceRoot() { return "/water"; }
                };

        Assertions.assertThrows(IllegalArgumentException.class, () -> client.registerService(wrongType));
    }

    @Test
    void unregisterService_withNon2xxResponse_logsWarning() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // Unregister a service that the test server returns 404 for
        // The server returns 404 for unknown paths → code should log warning, not throw
        Assertions.assertDoesNotThrow(() -> client.unregisterService("nonexistent-svc", "nonexistent-inst"));
    }

    @Test
    void heartbeat_withNon200Non204Response_returnsFalse() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // heartbeat for a service/instance that the server cannot find → returns 404
        // ServiceDiscoveryRegistryClientImpl.heartbeat(): 404 → remove from registeredInstances and return false
        boolean result = client.heartbeat("unknown-svc", "unknown-inst");
        Assertions.assertFalse(result);
    }

    @Test
    void heartbeat_with500Status_returnsFalseAndLogsWarning() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // The server returns 500 for "error-service/error-instance"
        // heartbeat: not 204/200, not 404 → logs warn and returns false
        boolean result = client.heartbeat("error-service", "error-instance");
        Assertions.assertFalse(result);
    }

    @Test
    void unregisterService_with200Status_removesFromRegistered() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // First register catalog-service/catalog-01
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "catalog-01", "/catalog", "2.1.0",
                "public-host", null
        );
        client.registerService(info);
        Assertions.assertTrue(client.isRegistered("catalog-01"));

        // Unregister returns 204 → removes from registeredInstances
        client.unregisterService("catalog-service", "catalog-01");
        Assertions.assertFalse(client.isRegistered("catalog-01"));
    }

    @Test
    void getServiceInfo_withNon200Response_returnsNull() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // GET for unknown id → server returns 404
        it.water.core.api.service.integration.discovery.DiscoverableServiceInfo info = client.getServiceInfo("9999");
        Assertions.assertNull(info);
    }

    @Test
    void getServiceInfo_withMalformedJson_returnsNull() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        // GET for special id "malformed" → server returns 200 with bad JSON
        it.water.core.api.service.integration.discovery.DiscoverableServiceInfo info = client.getServiceInfo("malformed");
        Assertions.assertNull(info);
    }

    @Test
    void isRegistered_returnsFalseForUnknownInstance() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        Assertions.assertFalse(client.isRegistered("never-registered"));
    }

    @Test
    void setup_normalizesTrailingSlashInRemoteUrl() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:8080/water/", "9090");
        Assertions.assertEquals("http://localhost:8080/water", client.getRemoteUrl());
    }

    @Test
    void setup_withNullRemoteUrl_setsNullRemoteUrl() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup(null, "9090");
        Assertions.assertNull(client.getRemoteUrl());
    }

    @Test
    void registerService_withRegistrationFailure_doesNotMarkAsRegistered() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // Point to a port that will refuse connections immediately
        client.setup("http://localhost:1/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9090", "fail-svc", "fail-01", "/water", "1.0.0",
                "localhost", null
        );
        // Should not throw; connection failure is caught and logged
        Assertions.assertDoesNotThrow(() -> client.registerService(info));
        Assertions.assertFalse(client.isRegistered("fail-01"));
    }

    @Test
    void registerService_afterSuccessfulPreviousRegistration_removesOldBeforeNewAttempt() {
        // Register once to populate registeredInstances, then re-register to verify remove+add
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9191", "catalog-service", "catalog-01", "/catalog", "2.1.0",
                "public-host", null
        );
        client.registerService(info);
        Assertions.assertTrue(client.isRegistered("catalog-01"));

        // Re-register the same instance: registerService removes it first, then re-adds on success
        client.registerService(info);
        Assertions.assertTrue(client.isRegistered("catalog-01"), "should be re-registered after second call");
    }

    // -----------------------------------------------------------------------
    // resolveEndpoint — remoteUrl already ends with the full path → returns it as-is
    // -----------------------------------------------------------------------

    @Test
    void resolveEndpoint_remoteUrlAlreadyEndsWithPath_returnsNormalized() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // Set remoteUrl to a value that already ends with the register path
        String registerPath = "/water/internal/serviceregistration/register";
        client.setup("http://localhost:" + serverPort + registerPath, "9191");

        String result = invokeResolveEndpoint(client, registerPath);
        Assertions.assertEquals("http://localhost:" + serverPort + registerPath, result);
    }

    // -----------------------------------------------------------------------
    // resolveEndpoint — remoteUrl ends with /water → appends path directly
    // -----------------------------------------------------------------------

    @Test
    void resolveEndpoint_remoteUrlEndsWithWater_appendsPathDirectly() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        String path = "/internal/serviceregistration/register";
        String result = invokeResolveEndpoint(client, path);
        Assertions.assertEquals("http://localhost:" + serverPort + "/water" + path, result);
    }

    // -----------------------------------------------------------------------
    // pickBackoff — index beyond array length → clamped to last element
    // -----------------------------------------------------------------------

    @Test
    void pickBackoff_indexBeyondArrayLength_returnsLastElement() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        long[] values = {100L, 200L, 400L};
        // index = 10, beyond length 3 → clamped to index 2 → 400
        long result = invokePickBackoff(client, values, 10);
        Assertions.assertEquals(400L, result);
    }

    // -----------------------------------------------------------------------
    // pickBackoff — negative value in array → returns 0
    // -----------------------------------------------------------------------

    @Test
    void pickBackoff_negativeValueInArray_returnsZero() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        long[] values = {-500L};
        long result = invokePickBackoff(client, values, 0);
        Assertions.assertEquals(0L, result);
    }

    // -----------------------------------------------------------------------
    // pickBackoff — empty array → returns 0
    // -----------------------------------------------------------------------

    @Test
    void pickBackoff_emptyArray_returnsZero() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        long result = invokePickBackoff(client, new long[0], 0);
        Assertions.assertEquals(0L, result);
    }

    // -----------------------------------------------------------------------
    // resolveHttpTimeout — null globalOptions → uses default 10s
    // -----------------------------------------------------------------------

    @Test
    void resolveHttpTimeout_nullGlobalOptions_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // globalOptions is null (not injected) → DEFAULT_HTTP_TIMEOUT_SECONDS = 10
        java.time.Duration timeout = invokeResolveHttpTimeout(client);
        Assertions.assertEquals(10L, timeout.toSeconds());
    }

    // -----------------------------------------------------------------------
    // resolveHttpTimeout — globalOptions with zero timeout → uses default
    // -----------------------------------------------------------------------

    @Test
    void resolveHttpTimeout_globalOptionsWithZeroTimeout_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setGlobalOptions(new ZeroTimeoutGlobalOptions());
        java.time.Duration timeout = invokeResolveHttpTimeout(client);
        Assertions.assertEquals(10L, timeout.toSeconds());
    }

    // -----------------------------------------------------------------------
    // resolveMaxAttempts — null globalOptions → uses default 3
    // -----------------------------------------------------------------------

    @Test
    void resolveMaxAttempts_nullGlobalOptions_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        int attempts = invokeResolveMaxAttempts(client);
        Assertions.assertEquals(3, attempts);
    }

    // -----------------------------------------------------------------------
    // resolveMaxAttempts — globalOptions with zero maxAttempts → uses default 3
    // -----------------------------------------------------------------------

    @Test
    void resolveMaxAttempts_globalOptionsWithZeroAttempts_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setGlobalOptions(new ZeroTimeoutGlobalOptions());
        int attempts = invokeResolveMaxAttempts(client);
        Assertions.assertEquals(3, attempts);
    }

    // -----------------------------------------------------------------------
    // resolveBackoffMs — null globalOptions → uses default backoff array
    // -----------------------------------------------------------------------

    @Test
    void resolveBackoffMs_nullGlobalOptions_returnsDefaultArray() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        long[] backoff = invokeResolveBackoffMs(client);
        Assertions.assertNotNull(backoff);
        Assertions.assertTrue(backoff.length > 0);
    }

    // -----------------------------------------------------------------------
    // resolveBackoffMs — globalOptions with null backoff → uses default
    // -----------------------------------------------------------------------

    @Test
    void resolveBackoffMs_globalOptionsWithNullBackoff_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setGlobalOptions(new NullBackoffGlobalOptions());
        long[] backoff = invokeResolveBackoffMs(client);
        Assertions.assertNotNull(backoff);
        // Should be the default backoff array
        Assertions.assertTrue(backoff.length > 0);
    }

    // -----------------------------------------------------------------------
    // resolveBackoffMs — globalOptions with empty array → uses default
    // -----------------------------------------------------------------------

    @Test
    void resolveBackoffMs_globalOptionsWithEmptyBackoff_returnsDefault() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setGlobalOptions(new EmptyBackoffGlobalOptions());
        long[] backoff = invokeResolveBackoffMs(client);
        Assertions.assertNotNull(backoff);
        Assertions.assertTrue(backoff.length > 0);
    }

    // -----------------------------------------------------------------------
    // resolveServicePort (in resolveRegistrationEndpoint) — info port blank, client port set
    // -----------------------------------------------------------------------

    @Test
    void resolveRegistrationEndpoint_infoportBlank_usesClientPort() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        // Setup sets this.port
        client.setup("http://localhost:" + serverPort + "/water", "7777");

        // info has blank port → resolveServicePort falls back to this.port
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "", "port-fallback-svc", "pf-01", "/water", "1.0.0",
                "localhost", null
        );
        String endpoint = invokeResolveRegistrationEndpoint(client, info);
        Assertions.assertTrue(endpoint.contains("7777"),
                "endpoint must use client port when info port is blank");
    }

    // -----------------------------------------------------------------------
    // resolveRegistrationEndpoint — host/port/root all blank → returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveRegistrationEndpoint_hostPortRootBlank_returnsEmpty() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "");

        // info with blank host, blank port, blank root
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "", "blank-endpoint-svc", "be-01", "", "1.0.0",
                "", null
        );
        String endpoint = invokeResolveRegistrationEndpoint(client, info);
        Assertions.assertEquals("", endpoint);
    }

    // -----------------------------------------------------------------------
    // resolveRegistrationEndpoint — only host blank → returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveRegistrationEndpoint_onlyHostBlank_returnsEmpty() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9090");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9090", "no-host-svc", "nh-01", "/water", "1.0.0",
                "", null  // blank host
        );
        String endpoint = invokeResolveRegistrationEndpoint(client, info);
        Assertions.assertEquals("", endpoint);
    }

    // -----------------------------------------------------------------------
    // resolveRegistrationEndpoint — only root blank → returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveRegistrationEndpoint_onlyRootBlank_returnsEmpty() throws Exception {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9090");

        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9090", "no-root-svc", "nr-01", "", "1.0.0",
                "localhost", null  // blank root
        );
        String endpoint = invokeResolveRegistrationEndpoint(client, info);
        Assertions.assertEquals("", endpoint);
    }

    // -----------------------------------------------------------------------
    // Reflection helpers
    // -----------------------------------------------------------------------

    private static String invokeResolveEndpoint(ServiceDiscoveryRegistryClientImpl client, String path) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("resolveEndpoint", String.class);
        method.setAccessible(true);
        return (String) method.invoke(client, path);
    }

    private static long invokePickBackoff(ServiceDiscoveryRegistryClientImpl client, long[] values, int index) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("pickBackoff", long[].class, int.class);
        method.setAccessible(true);
        return (long) method.invoke(client, values, index);
    }

    private static Duration invokeResolveHttpTimeout(ServiceDiscoveryRegistryClientImpl client) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("resolveHttpTimeout");
        method.setAccessible(true);
        return (Duration) method.invoke(client);
    }

    private static int invokeResolveMaxAttempts(ServiceDiscoveryRegistryClientImpl client) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("resolveMaxAttempts");
        method.setAccessible(true);
        return (int) method.invoke(client);
    }

    private static long[] invokeResolveBackoffMs(ServiceDiscoveryRegistryClientImpl client) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("resolveBackoffMs");
        method.setAccessible(true);
        return (long[]) method.invoke(client);
    }

    private static String invokeResolveRegistrationEndpoint(ServiceDiscoveryRegistryClientImpl client,
                                                             DiscoverableServiceInfoImpl info) throws Exception {
        java.lang.reflect.Method method = ServiceDiscoveryRegistryClientImpl.class
                .getDeclaredMethod("resolveRegistrationEndpoint", DiscoverableServiceInfoImpl.class);
        method.setAccessible(true);
        return (String) method.invoke(client, info);
    }

    // -----------------------------------------------------------------------
    // Stub GlobalOptions for edge-case tests
    // -----------------------------------------------------------------------

    private static final class ZeroTimeoutGlobalOptions implements ServiceDiscoveryGlobalOptions {
        @Override public String getDiscoveryUrl() { return ""; }
        @Override public String getDefaultHost() { return ""; }
        @Override public long getHeartbeatIntervalSeconds() { return 30L; }
        @Override public long getRegistrationRetryInitialDelaySeconds() { return 30L; }
        @Override public long getRegistrationRetryMaxDelaySeconds() { return 300L; }
        @Override public long getHttpTimeoutSeconds() { return 0L; }   // zero → uses default
        @Override public int getRegistrationMaxAttempts() { return 0; }  // zero → uses default
        @Override public long[] getRegistrationRetryBackoffMs() { return new long[]{2000L, 4000L}; }
    }

    private static final class NullBackoffGlobalOptions implements ServiceDiscoveryGlobalOptions {
        @Override public String getDiscoveryUrl() { return ""; }
        @Override public String getDefaultHost() { return ""; }
        @Override public long getHeartbeatIntervalSeconds() { return 30L; }
        @Override public long getRegistrationRetryInitialDelaySeconds() { return 30L; }
        @Override public long getRegistrationRetryMaxDelaySeconds() { return 300L; }
        @Override public long getHttpTimeoutSeconds() { return 10L; }
        @Override public int getRegistrationMaxAttempts() { return 3; }
        @Override public long[] getRegistrationRetryBackoffMs() { return null; }  // null → uses default
    }

    private static final class EmptyBackoffGlobalOptions implements ServiceDiscoveryGlobalOptions {
        @Override public String getDiscoveryUrl() { return ""; }
        @Override public String getDefaultHost() { return ""; }
        @Override public long getHeartbeatIntervalSeconds() { return 30L; }
        @Override public long getRegistrationRetryInitialDelaySeconds() { return 30L; }
        @Override public long getRegistrationRetryMaxDelaySeconds() { return 300L; }
        @Override public long getHttpTimeoutSeconds() { return 10L; }
        @Override public int getRegistrationMaxAttempts() { return 3; }
        @Override public long[] getRegistrationRetryBackoffMs() { return new long[0]; }  // empty → uses default
    }

    private void handleInternalDiscoveryRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(method) && path.endsWith("/register")) {
            postBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (postBody.get().contains("\"serviceName\":\"reject-service\"")) {
                writeResponse(exchange, 500, "rejected");
                return;
            }
            writeResponse(exchange, 200, "{\"id\":42}");
            return;
        }
        if ("DELETE".equals(method) && path.endsWith("/catalog-service/catalog-01")) {
            deletePath.set(path);
            writeResponse(exchange, 204, "");
            return;
        }
        if ("DELETE".equals(method) && path.endsWith("/missing-service/missing-instance")) {
            deletePath.set(path);
            writeResponse(exchange, 204, "");
            return;
        }
        if ("DELETE".equals(method) && path.endsWith("/error-service/error-01")) {
            deletePath.set(path);
            writeResponse(exchange, 500, "failed");
            return;
        }
        if ("PUT".equals(method) && path.endsWith("/heartbeat/catalog-service/catalog-01")) {
            heartbeatPath.set(path);
            writeResponse(exchange, 204, "");
            return;
        }
        if ("PUT".equals(method) && path.endsWith("/heartbeat/missing-service/missing-instance")) {
            heartbeatPath.set(path);
            writeResponse(exchange, 404, "");
            return;
        }
        if ("PUT".equals(method) && path.endsWith("/heartbeat/error-service/error-instance")) {
            heartbeatPath.set(path);
            writeResponse(exchange, 500, "internal server error");
            return;
        }
        // catch-all for tests expecting 404 (e.g. unregister unknown, heartbeat unknown)
        writeResponse(exchange, 404, "not found");
    }

    private void handlePublicDiscoveryRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(method) && path.endsWith("/42")) {
            writeResponse(exchange, 200, "{" +
                    "\"id\":42," +
                    "\"serviceName\":\"catalog-service\"," +
                    "\"serviceVersion\":\"2.1.0\"," +
                    "\"instanceId\":\"catalog-01\"," +
                    "\"endpoint\":\"http://remote-host:8181/water\"," +
                    "\"protocol\":\"http\"" +
                    "}");
            return;
        }
        if ("GET".equals(method) && path.endsWith("/80")) {
            writeResponse(exchange, 200, "{" +
                    "\"id\":80," +
                    "\"serviceName\":\"catalog-service\"," +
                    "\"serviceVersion\":\"2.1.0\"," +
                    "\"instanceId\":\"catalog-80\"," +
                    "\"endpoint\":\"http://remote-host/water\"," +
                    "\"protocol\":\"http\"" +
                    "}");
            return;
        }
        if ("GET".equals(method) && path.endsWith("/443")) {
            writeResponse(exchange, 200, "{" +
                    "\"id\":443," +
                    "\"serviceName\":\"secure-service\"," +
                    "\"serviceVersion\":\"2.1.0\"," +
                    "\"instanceId\":\"secure-443\"," +
                    "\"endpoint\":\"https://secure-host/secure\"," +
                    "\"protocol\":\"https\"" +
                    "}");
            return;
        }
        if ("GET".equals(method) && path.endsWith("/malformed")) {
            // Return 200 with malformed JSON to exercise parseServiceInfo error branch
            writeResponse(exchange, 200, "this is not json {{{");
            return;
        }
        writeResponse(exchange, 404, "not found");
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private ServiceDiscoveryGlobalOptions shortRetryOptions() {
        return new ServiceDiscoveryGlobalOptions() {
            @Override
            public String getDiscoveryUrl() {
                return "";
            }

            @Override
            public String getDefaultHost() {
                return "";
            }

            @Override
            public long getHeartbeatIntervalSeconds() {
                return 1L;
            }

            @Override
            public long getRegistrationRetryInitialDelaySeconds() {
                return 1L;
            }

            @Override
            public long getRegistrationRetryMaxDelaySeconds() {
                return 1L;
            }

            @Override
            public long getHttpTimeoutSeconds() {
                return 1L;
            }

            @Override
            public int getRegistrationMaxAttempts() {
                return 1;
            }

            @Override
            public long[] getRegistrationRetryBackoffMs() {
                return new long[]{0L};
            }
        };
    }
}
