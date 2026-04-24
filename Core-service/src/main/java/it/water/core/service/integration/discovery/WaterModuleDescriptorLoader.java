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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaterModuleDescriptorLoader {
    public static final String DESCRIPTOR_RESOURCE = "META-INF/water-descriptor.json";

    private static final Logger log = LoggerFactory.getLogger(WaterModuleDescriptorLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private WaterModuleDescriptorLoader() {
    }

    public static List<WaterServiceRegistrationDescriptor> loadServiceRegistrations(ClassLoader classLoader) {
        List<WaterServiceRegistrationDescriptor> loadedDescriptors = new ArrayList<>();
        if (classLoader == null) {
            return loadedDescriptors;
        }
        try {
            Enumeration<URL> resources = classLoader.getResources(DESCRIPTOR_RESOURCE);
            Map<String, WaterServiceRegistrationDescriptor> deduplicated = new LinkedHashMap<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream inputStream = resource.openStream()) {
                    WaterServiceRegistrationDescriptor descriptor = parse(inputStream);
                    if (descriptor == null) {
                        continue;
                    }
                    String key = descriptor.getModuleId() + ":" + descriptor.getServiceName() + ":" + descriptor.getRoot();
                    deduplicated.putIfAbsent(key, descriptor);
                } catch (Exception e) {
                    log.warn("Unable to read Water descriptor from {}: {}", resource, e.getMessage());
                }
            }
            loadedDescriptors.addAll(deduplicated.values());
        } catch (Exception e) {
            log.warn("Unable to enumerate Water descriptors: {}", e.getMessage());
        }
        return loadedDescriptors;
    }

    static WaterServiceRegistrationDescriptor parse(InputStream inputStream) throws Exception {
        JsonNode rootNode = objectMapper.readTree(inputStream);
        JsonNode serviceRegistrationNode = rootNode.path("runtime").path("serviceRegistration");
        if (serviceRegistrationNode.isMissingNode() || !serviceRegistrationNode.path("enabled").asBoolean(false)) {
            return null;
        }
        String moduleId = rootNode.path("moduleId").asText("");
        String serviceName = serviceRegistrationNode.path("serviceName").asText("");
        String serviceRoot = serviceRegistrationNode.path("root").asText("");
        if (moduleId.isBlank() || serviceName.isBlank() || serviceRoot.isBlank()) {
            return null;
        }
        String serviceVersion = serviceRegistrationNode.path("serviceVersion").asText("");
        String protocol = serviceRegistrationNode.path("protocol").asText("http");
        return new WaterServiceRegistrationDescriptor(moduleId, serviceName, serviceRoot, serviceVersion, protocol);
    }
}
