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
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.integration.discovery.DescriptorDrivenServiceRegistrationLifecycleManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@FrameworkComponent(services = DescriptorDrivenServiceRegistrationLifecycleManager.class)
public class DescriptorDrivenServiceRegistrationLifecycleManagerImpl implements DescriptorDrivenServiceRegistrationLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(DescriptorDrivenServiceRegistrationLifecycleManagerImpl.class);

    private final Map<String, DescriptorDrivenServiceRegistrationLifecycle> activeRegistrations = new LinkedHashMap<>();

    @Override
    public synchronized void activateDescriptorRegistrations(ComponentRegistry componentRegistry,
                                                             ApplicationProperties applicationProperties,
                                                             ClassLoader classLoader) {
        if (componentRegistry == null || applicationProperties == null || classLoader == null) {
            return;
        }

        List<WaterServiceRegistrationDescriptor> descriptors = WaterModuleDescriptorLoader.loadServiceRegistrations(classLoader);

        descriptors.forEach(descriptor -> {
            String descriptorKey = descriptorKey(descriptor);
            if (activeRegistrations.containsKey(descriptorKey)) {
                return;
            }

            log.debug("Activating descriptor-driven registration for service '{}'", descriptor.getServiceName());
            DescriptorDrivenServiceRegistrationLifecycle registration = new DescriptorDrivenServiceRegistrationLifecycle();
            registration.activate(componentRegistry, descriptor, applicationProperties);
            activeRegistrations.put(descriptorKey, registration);
        });
    }

    @OnDeactivate
    public synchronized void deactivate() {
        activeRegistrations.values().forEach(DescriptorDrivenServiceRegistrationLifecycle::deactivate);
        activeRegistrations.clear();
    }

    private String descriptorKey(WaterServiceRegistrationDescriptor descriptor) {
        return descriptor.getModuleId() + ":" + descriptor.getServiceName() + ":" + descriptor.getRoot();
    }
}
