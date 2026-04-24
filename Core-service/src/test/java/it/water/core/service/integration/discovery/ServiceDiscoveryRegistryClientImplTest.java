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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
        server.createContext("/water/api/serviceregistration", this::handlePublicDiscoveryRequest);
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
    void getServiceInfoUsesStandardPortDefaults() {
        ServiceDiscoveryRegistryClientImpl client = new ServiceDiscoveryRegistryClientImpl();
        client.setup("http://localhost:" + serverPort + "/water", "9191");

        DiscoverableServiceInfoImpl httpInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("80");
        Assertions.assertNotNull(httpInfo);
        Assertions.assertEquals("80", httpInfo.getServicePort());
        Assertions.assertEquals("/water", httpInfo.getServiceRoot());

        DiscoverableServiceInfoImpl httpsInfo = (DiscoverableServiceInfoImpl) client.getServiceInfo("443");
        Assertions.assertNotNull(httpsInfo);
        Assertions.assertEquals("443", httpsInfo.getServicePort());
        Assertions.assertEquals("/secure", httpsInfo.getServiceRoot());
    }

    private void handleInternalDiscoveryRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(method) && path.endsWith("/register")) {
            postBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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
        writeResponse(exchange, 404, "");
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
        writeResponse(exchange, 404, "");
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
