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

import it.water.core.api.bundle.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class DefaultServiceDiscoveryGlobalOptionsImplTest {

    // ---------- minimal ApplicationProperties backed by a HashMap ----------

    private static final class MapApplicationProperties implements ApplicationProperties {
        private final Map<String, String> props;

        MapApplicationProperties(Map<String, String> props) {
            this.props = props;
        }

        @Override
        public void setup() {
        }

        @Override
        public Object getProperty(String key) {
            return props.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            return props.containsKey(key);
        }

        @Override
        public void loadProperties(File file) {
        }

        @Override
        public void loadProperties(Properties p) {
        }

        @Override
        public void unloadProperties(File file) {
        }

        @Override
        public void unloadProperties(Properties p) {
        }
    }

    private static DefaultServiceDiscoveryGlobalOptionsImpl buildWith(Map<String, String> props) {
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        opts.setApplicationProperties(new MapApplicationProperties(props));
        return opts;
    }

    private static DefaultServiceDiscoveryGlobalOptionsImpl buildEmpty() {
        return buildWith(new HashMap<>());
    }

    private static DefaultServiceDiscoveryGlobalOptionsImpl buildNoProps() {
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        // no ApplicationProperties injected → all return defaults
        return opts;
    }

    // -----------------------------------------------------------------------
    // getDiscoveryUrl
    // -----------------------------------------------------------------------

    @Test
    void getDiscoveryUrl_returnsConfiguredValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "http://discovery.example.com");
        Assertions.assertEquals("http://discovery.example.com", buildWith(p).getDiscoveryUrl());
    }

    @Test
    void getDiscoveryUrl_returnsEmptyWhenNotConfigured() {
        Assertions.assertEquals("", buildEmpty().getDiscoveryUrl());
    }

    @Test
    void getDiscoveryUrl_returnsEmptyWhenNoApplicationProperties() {
        Assertions.assertEquals("", buildNoProps().getDiscoveryUrl());
    }

    // -----------------------------------------------------------------------
    // getDefaultHost
    // -----------------------------------------------------------------------

    @Test
    void getDefaultHost_returnsConfiguredValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_DEFAULT_HOST, "myhost.internal");
        Assertions.assertEquals("myhost.internal", buildWith(p).getDefaultHost());
    }

    @Test
    void getDefaultHost_returnsEmptyWhenNotConfigured() {
        Assertions.assertEquals("", buildEmpty().getDefaultHost());
    }

    @Test
    void getDefaultHost_returnsEmptyWhenNoApplicationProperties() {
        Assertions.assertEquals("", buildNoProps().getDefaultHost());
    }

    // -----------------------------------------------------------------------
    // getHeartbeatIntervalSeconds
    // -----------------------------------------------------------------------

    @Test
    void getHeartbeatIntervalSeconds_returnsDefault25WhenNotConfigured() {
        Assertions.assertEquals(25L, buildEmpty().getHeartbeatIntervalSeconds());
    }

    @Test
    void getHeartbeatIntervalSeconds_returnsConfiguredValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_HEARTBEAT_INTERVAL_SECONDS, "60");
        // getLongProperty delegates to ApplicationProperties.getPropertyOrDefault(key, long)
        // which calls getProperty and parses to long
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        opts.setApplicationProperties(new ApplicationProperties() {
            @Override public void setup() {}
            @Override public Object getProperty(String key) { return "60"; }
            @Override public boolean containsKey(String key) { return true; }
            @Override public void loadProperties(File file) {}
            @Override public void loadProperties(Properties pr) {}
            @Override public void unloadProperties(File file) {}
            @Override public void unloadProperties(Properties pr) {}
        });
        Assertions.assertEquals(60L, opts.getHeartbeatIntervalSeconds());
    }

    @Test
    void getHeartbeatIntervalSeconds_returnsDefaultWhenNoApplicationProperties() {
        Assertions.assertEquals(25L, buildNoProps().getHeartbeatIntervalSeconds());
    }

    // -----------------------------------------------------------------------
    // getRegistrationRetryInitialDelaySeconds
    // -----------------------------------------------------------------------

    @Test
    void getRegistrationRetryInitialDelaySeconds_returnsDefault30WhenNotConfigured() {
        Assertions.assertEquals(30L, buildEmpty().getRegistrationRetryInitialDelaySeconds());
    }

    @Test
    void getRegistrationRetryInitialDelaySeconds_returnsDefaultWhenNoApplicationProperties() {
        Assertions.assertEquals(30L, buildNoProps().getRegistrationRetryInitialDelaySeconds());
    }

    // -----------------------------------------------------------------------
    // getRegistrationRetryMaxDelaySeconds
    // -----------------------------------------------------------------------

    @Test
    void getRegistrationRetryMaxDelaySeconds_returnsDefault300WhenNotConfigured() {
        Assertions.assertEquals(300L, buildEmpty().getRegistrationRetryMaxDelaySeconds());
    }

    @Test
    void getRegistrationRetryMaxDelaySeconds_returnsDefaultWhenNoApplicationProperties() {
        Assertions.assertEquals(300L, buildNoProps().getRegistrationRetryMaxDelaySeconds());
    }

    // -----------------------------------------------------------------------
    // getHttpTimeoutSeconds
    // -----------------------------------------------------------------------

    @Test
    void getHttpTimeoutSeconds_returnsDefault10WhenNotConfigured() {
        Assertions.assertEquals(10L, buildEmpty().getHttpTimeoutSeconds());
    }

    @Test
    void getHttpTimeoutSeconds_returnsDefaultWhenNoApplicationProperties() {
        Assertions.assertEquals(10L, buildNoProps().getHttpTimeoutSeconds());
    }

    // -----------------------------------------------------------------------
    // getRegistrationMaxAttempts
    // -----------------------------------------------------------------------

    @Test
    void getRegistrationMaxAttempts_returnsDefault3WhenNotConfigured() {
        Assertions.assertEquals(3, buildEmpty().getRegistrationMaxAttempts());
    }

    @Test
    void getRegistrationMaxAttempts_returnsDefaultWhenNoApplicationProperties() {
        Assertions.assertEquals(3, buildNoProps().getRegistrationMaxAttempts());
    }

    @Test
    void getRegistrationMaxAttempts_returnsDefault3WhenValueIsZero() {
        // A stored value of 0 is invalid (<1) → must fall back to default 3
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        opts.setApplicationProperties(new ApplicationProperties() {
            @Override public void setup() {}
            @Override public Object getProperty(String key) { return "0"; }
            @Override public boolean containsKey(String key) { return true; }
            @Override public void loadProperties(File file) {}
            @Override public void loadProperties(Properties pr) {}
            @Override public void unloadProperties(File file) {}
            @Override public void unloadProperties(Properties pr) {}
        });
        Assertions.assertEquals(3, opts.getRegistrationMaxAttempts());
    }

    @Test
    void getRegistrationMaxAttempts_returnsDefault3WhenValueExceedsIntMax() {
        // value > Integer.MAX_VALUE → must fall back to default 3
        long tooBig = (long) Integer.MAX_VALUE + 1L;
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        opts.setApplicationProperties(new ApplicationProperties() {
            @Override public void setup() {}
            @Override public Object getProperty(String key) { return String.valueOf(tooBig); }
            @Override public boolean containsKey(String key) { return true; }
            @Override public void loadProperties(File file) {}
            @Override public void loadProperties(Properties pr) {}
            @Override public void unloadProperties(File file) {}
            @Override public void unloadProperties(Properties pr) {}
        });
        Assertions.assertEquals(3, opts.getRegistrationMaxAttempts());
    }

    @Test
    void getRegistrationMaxAttempts_returnsValidPositiveValue() {
        DefaultServiceDiscoveryGlobalOptionsImpl opts = new DefaultServiceDiscoveryGlobalOptionsImpl();
        opts.setApplicationProperties(new ApplicationProperties() {
            @Override public void setup() {}
            @Override public Object getProperty(String key) { return "5"; }
            @Override public boolean containsKey(String key) { return true; }
            @Override public void loadProperties(File file) {}
            @Override public void loadProperties(Properties pr) {}
            @Override public void unloadProperties(File file) {}
            @Override public void unloadProperties(Properties pr) {}
        });
        Assertions.assertEquals(5, opts.getRegistrationMaxAttempts());
    }

    // -----------------------------------------------------------------------
    // getRegistrationRetryBackoffMs
    // -----------------------------------------------------------------------

    @Test
    void getRegistrationRetryBackoffMs_returnsDefaultWhenNotConfigured() {
        long[] result = buildEmpty().getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{2000L, 4000L, 8000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_returnsDefaultWhenNoApplicationProperties() {
        long[] result = buildNoProps().getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{2000L, 4000L, 8000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_parsesCommaSeparatedValues() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "500,1000,2000");
        long[] result = buildWith(p).getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{500L, 1000L, 2000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_returnsDefaultOnNonNumericValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "500,bad,2000");
        long[] result = buildWith(p).getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{2000L, 4000L, 8000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_returnsDefaultOnNegativeValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "500,-100,2000");
        long[] result = buildWith(p).getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{2000L, 4000L, 8000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_returnsDefaultWhenBlankAfterTrim() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "   ");
        long[] result = buildWith(p).getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{2000L, 4000L, 8000L}, result);
    }

    @Test
    void getRegistrationRetryBackoffMs_parsesSingleValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "1000");
        long[] result = buildWith(p).getRegistrationRetryBackoffMs();
        Assertions.assertArrayEquals(new long[]{1000L}, result);
    }

    // -----------------------------------------------------------------------
    // getStringProperty: null value returned from ApplicationProperties → ""
    // -----------------------------------------------------------------------

    @Test
    void getDiscoveryUrl_trimsWhitespaceFromConfiguredValue() {
        Map<String, String> p = new HashMap<>();
        p.put(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "  http://trimmed.example.com  ");
        Assertions.assertEquals("http://trimmed.example.com", buildWith(p).getDiscoveryUrl());
    }
}
