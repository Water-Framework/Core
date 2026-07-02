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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DiscoverableServiceInfoImplTest {

    // -----------------------------------------------------------------------
    // 6-arg constructor (no host / endpoint)
    // -----------------------------------------------------------------------

    @Test
    void sixArgConstructor_setsAllFieldsAndNullsHostAndEndpoint() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "8080", "my-service", "inst-1", "/root", "2.0.0"
        );
        Assertions.assertEquals("http", info.getServiceProtocol());
        Assertions.assertEquals("8080", info.getServicePort());
        Assertions.assertEquals("my-service", info.getServiceId());
        Assertions.assertEquals("inst-1", info.getServiceInstanceId());
        Assertions.assertEquals("/root", info.getServiceRoot());
        Assertions.assertEquals("2.0.0", info.getServiceVersion());
        Assertions.assertNull(info.getServiceHost());
        Assertions.assertNull(info.getServiceEndpoint());
    }

    // -----------------------------------------------------------------------
    // 8-arg constructor
    // -----------------------------------------------------------------------

    @Test
    void eightArgConstructor_setsAllFields() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "https", "443", "secure-svc", "inst-2", "/api", "3.0.0",
                "secure.example.com", "https://secure.example.com/api"
        );
        Assertions.assertEquals("https", info.getServiceProtocol());
        Assertions.assertEquals("443", info.getServicePort());
        Assertions.assertEquals("secure-svc", info.getServiceId());
        Assertions.assertEquals("inst-2", info.getServiceInstanceId());
        Assertions.assertEquals("/api", info.getServiceRoot());
        Assertions.assertEquals("3.0.0", info.getServiceVersion());
        Assertions.assertEquals("secure.example.com", info.getServiceHost());
        Assertions.assertEquals("https://secure.example.com/api", info.getServiceEndpoint());
    }

    // -----------------------------------------------------------------------
    // toString — branch: serviceEndpoint non-blank → use explicit endpoint
    // -----------------------------------------------------------------------

    @Test
    void toString_withExplicitEndpoint_usesEndpointField() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "9090", "svc", "inst-3", "/svc", "1.0.0",
                "host.internal", "http://explicit.example.com/svc"
        );
        String str = info.toString();
        Assertions.assertTrue(str.contains("svc"), "should contain service id");
        Assertions.assertTrue(str.contains("inst-3"), "should contain instance id");
        Assertions.assertTrue(str.contains("http://explicit.example.com/svc"), "should contain explicit endpoint");
    }

    // -----------------------------------------------------------------------
    // toString — branch: serviceEndpoint null/blank → build from host/port/root
    // -----------------------------------------------------------------------

    @Test
    void toString_withoutExplicitEndpoint_buildsEndpointFromHostPortRoot() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "8181", "catalog", "inst-4", "/catalog", "1.0.0",
                "catalog-host", null
        );
        String str = info.toString();
        Assertions.assertTrue(str.contains("http://catalog-host:8181/catalog"), "should build endpoint from parts");
    }

    // -----------------------------------------------------------------------
    // toString — branch: serviceHost null/blank → "host" placeholder
    // -----------------------------------------------------------------------

    @Test
    void toString_withBlankHost_usesHostPlaceholder() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "7070", "svc2", "inst-5", "/svc2", "1.0.0",
                "", null
        );
        String str = info.toString();
        // host is blank → "host" literal
        Assertions.assertTrue(str.contains("http://host:7070/svc2"), "should use 'host' placeholder when serviceHost is blank");
    }

    @Test
    void toString_withNullHost_usesHostPlaceholder() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "7070", "svc3", "inst-6", "/svc3", "1.0.0",
                null, null
        );
        String str = info.toString();
        Assertions.assertTrue(str.contains("http://host:7070/svc3"));
    }

    // -----------------------------------------------------------------------
    // Getters consistency — 6-arg variant via interface methods
    // -----------------------------------------------------------------------

    @Test
    void getServiceId_equalsServiceName() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "8080", "name-service", "i1", "/n", "1.0.0"
        );
        Assertions.assertEquals("name-service", info.getServiceId());
    }

    @Test
    void getServiceInstanceId_returnsExpected() {
        DiscoverableServiceInfoImpl info = new DiscoverableServiceInfoImpl(
                "http", "8080", "svc", "myInstanceId", "/root", "1.0.0"
        );
        Assertions.assertEquals("myInstanceId", info.getServiceInstanceId());
    }
}
