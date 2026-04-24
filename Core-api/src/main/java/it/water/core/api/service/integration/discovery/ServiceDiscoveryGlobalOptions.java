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

package it.water.core.api.service.integration.discovery;

import it.water.core.api.service.Service;

/**
 * Runtime-wide infrastructure options for ServiceDiscovery integration.
 * <p>
 * These values are shared across all discoverable modules running in the same
 * runtime: they describe "how the discovery infrastructure behaves" (HTTP
 * timeouts, retry policy, heartbeat cadence) and act as fallback for values
 * that a single module does not want to (or cannot) specify (URL of the
 * ServiceDiscovery, default host).
 * <p>
 * Identity of a single service (name, version, instanceId, root, advertised
 * endpoint, ...) belongs to {@link ServiceRegistrationOptions}, NOT here.
 */
public interface ServiceDiscoveryGlobalOptions extends Service {

    /**
     * Fallback URL of the ServiceDiscovery REST endpoint used when a module
     * does not specify its own. Empty string means "no fallback available":
     * modules that also return an empty discoveryUrl will be skipped.
     */
    String getDiscoveryUrl();

    /**
     * Fallback host advertised by services that do not specify their own.
     * Empty string means "resolve local hostname at registration time".
     */
    String getDefaultHost();

    /**
     * Period between two heartbeat calls from each registered service.
     */
    long getHeartbeatIntervalSeconds();

    /**
     * Initial delay before retrying a failed registration (exponential backoff
     * starts from this value up to the max below).
     */
    long getRegistrationRetryInitialDelaySeconds();

    /**
     * Upper bound of the exponential backoff delay between registration retries.
     */
    long getRegistrationRetryMaxDelaySeconds();

    /**
     * Connect timeout applied to every HTTP call from the client to the
     * ServiceDiscovery REST endpoint.
     */
    long getHttpTimeoutSeconds();

    /**
     * Maximum number of attempts performed by the HTTP client for a single
     * register request before giving up and scheduling a retry via
     * {@link #getRegistrationRetryInitialDelaySeconds()}.
     */
    int getRegistrationMaxAttempts();

    /**
     * Back-off delays (in milliseconds) applied between the in-request retry
     * attempts performed by the HTTP client. The array length should match
     * {@link #getRegistrationMaxAttempts()} - 1: if shorter, the last value
     * is repeated; if longer, extra values are ignored.
     */
    long[] getRegistrationRetryBackoffMs();
}
