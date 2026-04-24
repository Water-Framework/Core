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

/**
 * Property keys used by {@link DefaultServiceDiscoveryGlobalOptionsImpl}.
 * These are runtime-wide keys consumed by the ServiceDiscovery infrastructure;
 * they never describe the identity of a single service. Module identity
 * (service name and real root) lives in code inside each module's
 * {@code ServiceRegistrationOptions} implementation.
 */
public abstract class ServiceDiscoveryGlobalConstants {

    public static final String PROP_DISCOVERY_URL =
            "water.discovery.url";
    public static final String PROP_DEFAULT_HOST =
            "water.discovery.default-host";
    public static final String PROP_HEARTBEAT_INTERVAL_SECONDS =
            "water.discovery.heartbeat.interval.seconds";
    public static final String PROP_REGISTRATION_RETRY_INITIAL_DELAY_SECONDS =
            "water.discovery.registration.retry.initial.delay.seconds";
    public static final String PROP_REGISTRATION_RETRY_MAX_DELAY_SECONDS =
            "water.discovery.registration.retry.max.delay.seconds";
    public static final String PROP_HTTP_TIMEOUT_SECONDS =
            "water.discovery.http.timeout.seconds";
    public static final String PROP_REGISTRATION_MAX_ATTEMPTS =
            "water.discovery.registration.max.attempts";
    public static final String PROP_REGISTRATION_RETRY_BACKOFF_MS =
            "water.discovery.registration.retry.backoff.ms";

    private ServiceDiscoveryGlobalConstants() {
        // prevent instantiation
    }
}
