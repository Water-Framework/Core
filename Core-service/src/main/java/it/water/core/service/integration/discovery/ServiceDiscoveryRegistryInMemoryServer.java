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

import it.water.core.api.service.integration.discovery.ServiceDiscoveryServerProperties;
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.interceptors.annotations.FrameworkComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Aristide Cittadino
 * Default Service Discovery with lowest priority in order to support simple in memory server discovery.
 */
@FrameworkComponent(priority = 0, properties = {ServiceDiscoveryServerProperties.SERVICE_DISCOVERY_SERVER_IMPLEMENTATION_PROP + "=" + ServiceDiscoveryServerProperties.SERVICE_DISCOVERY_IN_MEMORY_SERVER_IMPLEMENTATION})
public class ServiceDiscoveryRegistryInMemoryServer implements it.water.core.api.service.integration.discovery.ServiceDiscoveryRegistryServer {
    Map<String, DiscoverableServiceInfo> serviceInfoMap = new HashMap<>();

    @Override
    public void registerService(DiscoverableServiceInfo registration) {
        serviceInfoMap.put(registration.getServiceId(), registration);
    }

    @Override
    public void unregisterService(String id) {
        serviceInfoMap.remove(id);
    }

    @Override
    public DiscoverableServiceInfo getServiceInfo(String id) {
        return serviceInfoMap.get(id);
    }
}
