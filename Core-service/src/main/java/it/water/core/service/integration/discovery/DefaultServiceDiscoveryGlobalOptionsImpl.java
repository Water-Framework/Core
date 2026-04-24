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
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ServiceDiscoveryGlobalOptions} backed by
 * {@link ApplicationProperties}. Exposes sensible defaults so the framework
 * remains fully operational without any explicit configuration.
 */
@FrameworkComponent
public class DefaultServiceDiscoveryGlobalOptionsImpl implements ServiceDiscoveryGlobalOptions {

    private static final Logger log = LoggerFactory.getLogger(DefaultServiceDiscoveryGlobalOptionsImpl.class);

    private static final long DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 25L;
    private static final long DEFAULT_RETRY_INITIAL_DELAY_SECONDS = 30L;
    private static final long DEFAULT_RETRY_MAX_DELAY_SECONDS = 300L;
    private static final long DEFAULT_HTTP_TIMEOUT_SECONDS = 10L;
    private static final int DEFAULT_REGISTRATION_MAX_ATTEMPTS = 3;
    private static final long[] DEFAULT_RETRY_BACKOFF_MS = {2000L, 4000L, 8000L};

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Override
    public String getDiscoveryUrl() {
        return getStringProperty(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "");
    }

    @Override
    public String getDefaultHost() {
        return getStringProperty(ServiceDiscoveryGlobalConstants.PROP_DEFAULT_HOST, "");
    }

    @Override
    public long getHeartbeatIntervalSeconds() {
        return getLongProperty(
                ServiceDiscoveryGlobalConstants.PROP_HEARTBEAT_INTERVAL_SECONDS,
                DEFAULT_HEARTBEAT_INTERVAL_SECONDS);
    }

    @Override
    public long getRegistrationRetryInitialDelaySeconds() {
        return getLongProperty(
                ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_INITIAL_DELAY_SECONDS,
                DEFAULT_RETRY_INITIAL_DELAY_SECONDS);
    }

    @Override
    public long getRegistrationRetryMaxDelaySeconds() {
        return getLongProperty(
                ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_MAX_DELAY_SECONDS,
                DEFAULT_RETRY_MAX_DELAY_SECONDS);
    }

    @Override
    public long getHttpTimeoutSeconds() {
        return getLongProperty(
                ServiceDiscoveryGlobalConstants.PROP_HTTP_TIMEOUT_SECONDS,
                DEFAULT_HTTP_TIMEOUT_SECONDS);
    }

    @Override
    public int getRegistrationMaxAttempts() {
        long value = getLongProperty(
                ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_MAX_ATTEMPTS,
                DEFAULT_REGISTRATION_MAX_ATTEMPTS);
        if (value < 1L || value > Integer.MAX_VALUE) {
            log.warn("Invalid value for {} ({}), falling back to default {}",
                    ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_MAX_ATTEMPTS, value, DEFAULT_REGISTRATION_MAX_ATTEMPTS);
            return DEFAULT_REGISTRATION_MAX_ATTEMPTS;
        }
        return (int) value;
    }

    @Override
    public long[] getRegistrationRetryBackoffMs() {
        String raw = getStringProperty(
                ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS, "");
        if (raw.isBlank()) {
            return DEFAULT_RETRY_BACKOFF_MS.clone();
        }
        String[] parts = raw.split(",");
        long[] parsed = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String segment = parts[i].trim();
            try {
                long v = Long.parseLong(segment);
                if (v < 0L) {
                    log.warn("Negative backoff delay '{}' in {}, falling back to defaults",
                            segment, ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS);
                    return DEFAULT_RETRY_BACKOFF_MS.clone();
                }
                parsed[i] = v;
            } catch (NumberFormatException e) {
                log.warn("Invalid backoff value '{}' in {}, falling back to defaults",
                        segment, ServiceDiscoveryGlobalConstants.PROP_REGISTRATION_RETRY_BACKOFF_MS);
                return DEFAULT_RETRY_BACKOFF_MS.clone();
            }
        }
        return parsed;
    }

    private String getStringProperty(String key, String defaultValue) {
        if (applicationProperties == null) {
            return defaultValue;
        }
        String value = applicationProperties.getPropertyOrDefault(key, defaultValue);
        return value == null ? "" : value.trim();
    }

    private long getLongProperty(String key, long defaultValue) {
        if (applicationProperties == null) {
            return defaultValue;
        }
        return applicationProperties.getPropertyOrDefault(key, defaultValue);
    }
}
