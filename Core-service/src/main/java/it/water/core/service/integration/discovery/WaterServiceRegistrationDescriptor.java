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

public class WaterServiceRegistrationDescriptor {
    private final String moduleId;
    private final String serviceName;
    private final String root;
    private final String serviceVersion;
    private final String protocol;

    public WaterServiceRegistrationDescriptor(String moduleId,
                                              String serviceName,
                                              String root,
                                              String serviceVersion,
                                              String protocol) {
        this.moduleId = moduleId;
        this.serviceName = serviceName;
        this.root = root;
        this.serviceVersion = serviceVersion;
        this.protocol = protocol;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getRoot() {
        return root;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getProtocol() {
        return protocol;
    }
}
