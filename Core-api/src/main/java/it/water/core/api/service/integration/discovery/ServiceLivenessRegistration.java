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

public class ServiceLivenessRegistration {
    private final String serviceName;
    private final String instanceId;
    private final String serviceVersion;
    private final String protocol;
    private final String root;
    private final String endpoint;
    private final String serviceHost;
    private final String servicePort;
    private final String nodeId;
    private final String layer;

    public ServiceLivenessRegistration(String serviceName,
                                       String instanceId,
                                       String serviceVersion,
                                       String protocol,
                                       String root,
                                       String endpoint,
                                       String serviceHost,
                                       String servicePort,
                                       String nodeId,
                                       String layer) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.serviceVersion = serviceVersion;
        this.protocol = protocol;
        this.root = root;
        this.endpoint = endpoint;
        this.serviceHost = serviceHost;
        this.servicePort = servicePort;
        this.nodeId = nodeId;
        this.layer = layer;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRoot() {
        return root;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public String getServicePort() {
        return servicePort;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getLayer() {
        return layer;
    }
}
