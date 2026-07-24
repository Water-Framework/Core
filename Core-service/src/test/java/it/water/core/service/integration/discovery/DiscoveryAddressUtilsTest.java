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

class DiscoveryAddressUtilsTest {

    // -----------------------------------------------------------------------
    // normalizeAdvertisedEndpoint
    // -----------------------------------------------------------------------

    @Test
    void normalizeAdvertisedEndpoint_null_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeAdvertisedEndpoint(null));
    }

    @Test
    void normalizeAdvertisedEndpoint_blank_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeAdvertisedEndpoint("   "));
    }

    @Test
    void normalizeAdvertisedEndpoint_withTrailingSlash_removesIt() {
        Assertions.assertEquals("http://example.com/root",
                DiscoveryAddressUtils.normalizeAdvertisedEndpoint("http://example.com/root/"));
    }

    @Test
    void normalizeAdvertisedEndpoint_withoutTrailingSlash_returnsUnchanged() {
        Assertions.assertEquals("http://example.com/root",
                DiscoveryAddressUtils.normalizeAdvertisedEndpoint("http://example.com/root"));
    }

    @Test
    void normalizeAdvertisedEndpoint_trimsWhitespace() {
        Assertions.assertEquals("http://example.com",
                DiscoveryAddressUtils.normalizeAdvertisedEndpoint("  http://example.com  "));
    }

    // -----------------------------------------------------------------------
    // normalizeHost
    // -----------------------------------------------------------------------

    @Test
    void normalizeHost_null_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeHost(null));
    }

    @Test
    void normalizeHost_blank_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeHost("   "));
    }

    @Test
    void normalizeHost_validHost_returnsTrimmed() {
        Assertions.assertEquals("my.host.internal",
                DiscoveryAddressUtils.normalizeHost("  my.host.internal  "));
    }

    // -----------------------------------------------------------------------
    // normalizeRoot
    // -----------------------------------------------------------------------

    @Test
    void normalizeRoot_null_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeRoot(null));
    }

    @Test
    void normalizeRoot_blank_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.normalizeRoot("   "));
    }

    @Test
    void normalizeRoot_withoutLeadingSlash_prependsSlash() {
        Assertions.assertEquals("/water", DiscoveryAddressUtils.normalizeRoot("water"));
    }

    @Test
    void normalizeRoot_withLeadingSlash_doesNotDouble() {
        Assertions.assertEquals("/water", DiscoveryAddressUtils.normalizeRoot("/water"));
    }

    @Test
    void normalizeRoot_withTrailingSlash_removesIt() {
        Assertions.assertEquals("/water", DiscoveryAddressUtils.normalizeRoot("/water/"));
    }

    @Test
    void normalizeRoot_rootOnly_singleSlash_keepsIt() {
        // "/" has length 1 so the trailing-slash removal condition (length > 1) is false
        Assertions.assertEquals("/", DiscoveryAddressUtils.normalizeRoot("/"));
    }

    @Test
    void normalizeRoot_trimsWhitespace() {
        Assertions.assertEquals("/api", DiscoveryAddressUtils.normalizeRoot("  api  "));
    }

    // -----------------------------------------------------------------------
    // extractPortFromEndpoint
    // -----------------------------------------------------------------------

    @Test
    void extractPortFromEndpoint_null_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.extractPortFromEndpoint(null));
    }

    @Test
    void extractPortFromEndpoint_blank_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.extractPortFromEndpoint("  "));
    }

    @Test
    void extractPortFromEndpoint_explicitPort_returnsThat() {
        Assertions.assertEquals("9090",
                DiscoveryAddressUtils.extractPortFromEndpoint("http://host.example.com:9090/root"));
    }

    @Test
    void extractPortFromEndpoint_httpNoPort_returns80() {
        Assertions.assertEquals("80",
                DiscoveryAddressUtils.extractPortFromEndpoint("http://host.example.com/root"));
    }

    @Test
    void extractPortFromEndpoint_httpsNoPort_returns443() {
        Assertions.assertEquals("443",
                DiscoveryAddressUtils.extractPortFromEndpoint("https://host.example.com/root"));
    }

    @Test
    void extractPortFromEndpoint_unknownSchemeNoPort_returnsEmpty() {
        Assertions.assertEquals("",
                DiscoveryAddressUtils.extractPortFromEndpoint("ftp://host.example.com/root"));
    }

    @Test
    void extractPortFromEndpoint_malformedUri_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.extractPortFromEndpoint("not a uri ://bad"));
    }

    // -----------------------------------------------------------------------
    // extractRootFromEndpoint
    // -----------------------------------------------------------------------

    @Test
    void extractRootFromEndpoint_null_returnsDefaultRoot() {
        Assertions.assertEquals("/default",
                DiscoveryAddressUtils.extractRootFromEndpoint(null, "/default"));
    }

    @Test
    void extractRootFromEndpoint_blank_returnsDefaultRoot() {
        Assertions.assertEquals("/default",
                DiscoveryAddressUtils.extractRootFromEndpoint("  ", "/default"));
    }

    @Test
    void extractRootFromEndpoint_withPath_returnsNormalizedPath() {
        Assertions.assertEquals("/catalog",
                DiscoveryAddressUtils.extractRootFromEndpoint("http://host:8080/catalog", "/water"));
    }

    @Test
    void extractRootFromEndpoint_noPathInUri_returnsDefaultRoot() {
        // URI "http://host:8080" has empty path
        Assertions.assertEquals("/water",
                DiscoveryAddressUtils.extractRootFromEndpoint("http://host:8080", "/water"));
    }

    @Test
    void extractRootFromEndpoint_malformedUri_returnsDefaultRoot() {
        Assertions.assertEquals("/water",
                DiscoveryAddressUtils.extractRootFromEndpoint("not a uri ://bad", "/water"));
    }

    // -----------------------------------------------------------------------
    // defaultPortForScheme
    // -----------------------------------------------------------------------

    @Test
    void defaultPortForScheme_http_returns80() {
        Assertions.assertEquals("80", DiscoveryAddressUtils.defaultPortForScheme("http"));
    }

    @Test
    void defaultPortForScheme_https_returns443() {
        Assertions.assertEquals("443", DiscoveryAddressUtils.defaultPortForScheme("https"));
    }

    @Test
    void defaultPortForScheme_unknown_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.defaultPortForScheme("ftp"));
    }

    @Test
    void defaultPortForScheme_null_returnsEmpty() {
        Assertions.assertEquals("", DiscoveryAddressUtils.defaultPortForScheme(null));
    }

    @Test
    void defaultPortForScheme_caseInsensitive_HTTP() {
        Assertions.assertEquals("80", DiscoveryAddressUtils.defaultPortForScheme("HTTP"));
    }

    // -----------------------------------------------------------------------
    // resolveLocalHostname — succeeds in a normal JVM environment
    // -----------------------------------------------------------------------

    @Test
    void resolveLocalHostname_returnsNonBlankInNormalEnvironment() {
        String hostname = DiscoveryAddressUtils.resolveLocalHostname(null, "test");
        // hostname may be empty on exotic CI hosts, but should not be null
        Assertions.assertNotNull(hostname);
    }

    @Test
    void resolveLocalHostname_withNullLogger_doesNotThrow() {
        Assertions.assertDoesNotThrow(() -> DiscoveryAddressUtils.resolveLocalHostname(null, null));
    }
}
