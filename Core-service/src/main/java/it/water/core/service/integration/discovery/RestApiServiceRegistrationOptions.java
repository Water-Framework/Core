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

/**
 * Runtime registration options derived from ApplicationProperties and the discovered REST root.
 */
public class RestApiServiceRegistrationOptions implements ServiceRegistrationOptions {
    private static final String GLOBAL_OSGI_PORT = "org.osgi.service.http.port";
    private static final String GLOBAL_SPRING_PORT = "server.port";
    private static final String REST_ROOT_CONTEXT = "water.rest.root.context";
    private static final String SPRING_CONTEXT_PATH = "server.servlet.context-path";
    private static final String CXF_CONTEXT_PATH = "org.apache.cxf.servlet.context";
    private static final String DEFAULT_REST_ROOT_CONTEXT = "/water";

    private final String serviceName;
    private final String root;
    private final ApplicationProperties applicationProperties;

    public RestApiServiceRegistrationOptions(String serviceName,
                                             String root,
                                             ApplicationProperties applicationProperties) {
        this.serviceName = serviceName;
        this.root = root;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getDiscoveryUrl() {
        return "";
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getServiceVersion() {
        return "1.0.0";
    }

    @Override
    public String getInstanceId() {
        return "";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public String getRoot() {
        String normalizedRoot = DiscoveryAddressUtils.normalizeRoot(root);
        String context = resolveRestRootContext();
        if (context.isBlank() || "/".equals(context) || normalizedRoot.startsWith(context + "/") || normalizedRoot.equals(context)) {
            return normalizedRoot;
        }
        return context + normalizedRoot;
    }

    @Override
    public String getAdvertisedEndpoint() {
        return "";
    }

    @Override
    public String getServicePort() {
        String osgiPort = getProperty(GLOBAL_OSGI_PORT, "");
        if (!osgiPort.isBlank()) {
            return osgiPort;
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

    private String resolveRestRootContext() {
        String context = getProperty(REST_ROOT_CONTEXT, "");
        if (context.isBlank()) {
            context = getProperty(SPRING_CONTEXT_PATH, "");
        }
        if (context.isBlank()) {
            context = getProperty(CXF_CONTEXT_PATH, "");
        }
        return DiscoveryAddressUtils.normalizeRoot(context.isBlank() ? DEFAULT_REST_ROOT_CONTEXT : context);
    }
}
