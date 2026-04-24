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
import it.water.core.api.service.integration.discovery.ServiceRegistrationOptions;

public class DescriptorBackedServiceRegistrationOptions implements ServiceRegistrationOptions {
    private static final String GLOBAL_OSGI_PORT = "org.osgi.service.http.port";
    private static final String GLOBAL_SPRING_PORT = "server.port";

    private final WaterServiceRegistrationDescriptor descriptor;
    private final ApplicationProperties applicationProperties;

    public DescriptorBackedServiceRegistrationOptions(WaterServiceRegistrationDescriptor descriptor,
                                                      ApplicationProperties applicationProperties) {
        this.descriptor = descriptor;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getDiscoveryUrl() {
        return getProperty(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "");
    }

    @Override
    public String getServiceName() {
        return descriptor.getServiceName();
    }

    @Override
    public String getServiceVersion() {
        return descriptor.getServiceVersion();
    }

    @Override
    public String getInstanceId() {
        return "";
    }

    @Override
    public String getProtocol() {
        return descriptor.getProtocol();
    }

    @Override
    public String getRoot() {
        return descriptor.getRoot();
    }

    @Override
    public String getAdvertisedEndpoint() {
        return "";
    }

    @Override
    public String getServicePort() {
        String port = getProperty(GLOBAL_OSGI_PORT, "");
        if (!port.isBlank()) {
            return port;
        }
        return getProperty(GLOBAL_SPRING_PORT, "");
    }

    @Override
    public String getServiceHost() {
        return "";
    }

    private String getProperty(String key, String defaultValue) {
        if (applicationProperties == null) {
            return defaultValue;
        }
        return applicationProperties.getPropertyOrDefault(key, defaultValue);
    }
}
