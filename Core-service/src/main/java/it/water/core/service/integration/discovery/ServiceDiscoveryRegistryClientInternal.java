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

import it.water.core.api.service.integration.discovery.ServiceDiscoveryRegistryClient;

/**
 * Internal runtime-facing contract used by Core service registration lifecycle.
 * <p>
 * It extends the public API client without expanding {@code Core-api}. The
 * extra methods below are needed only by the framework runtime to verify
 * whether registration really succeeded and to push periodic heartbeats.
 * The two-arg {@code unregisterService(serviceName, instanceId)} lives in
 * the public {@link ServiceDiscoveryRegistryClient} interface, so it is not
 * redeclared here.
 */
public interface ServiceDiscoveryRegistryClientInternal extends ServiceDiscoveryRegistryClient {
    boolean isRegistered(String instanceId);

    boolean heartbeat(String serviceName, String instanceId);
}
