package it.water.core.api.service.cluster;

import it.water.core.api.service.Service;

/**
 * @Author Aristide Cittadino
 * This class maps all properties related to a single network node which exposes some services.
 * It helps to mangge a distributed cluster with auto discover and, eventually, for service discovery.
 * Indeed it's crucial for every functionality which involves network information about the cluster nodes.
 * <p>
 * Main informations are:
 * - Node id - node instance id
 * - Layer - string which identifies the node inside a logical category es. microservices, integration, so you can create different cluster with different layers
 * - Host
 * - Port
 */
public interface ClusterNodeOptions extends Service {
    String PROP_NODE_ID = "water.core.api.service.cluster.node.id";
    String PROP_LAYER_ID = "water.core.api.service.cluster.node.layer.id";
    String PROP_IP = "water.core.api.service.cluster.node.ip";
    String PROP_HOST = "water.core.api.service.cluster.node.host";
    String PROP_USE_IP = "water.core.api.service.cluster.node.useIp";
    String PROP_CLUSTER_MODE_ENABLED = "water.core.api.service.cluster.mode.enabled";

    /**
     * Activate the cluster mode
     * @return
     */
    boolean clusterModeEnabled();
    /**
     * service id
     *
     * @return
     */
    String getNodeId();

    /**
     * @return
     */
    String getLayer();

    /**
     * @return
     */
    String getIp();

    /**
     * @return
     */
    String getHost();

    /**
     * Force the use of the ip inside the cluster registration not hosts, which is the default
     */
    boolean useIpInClusterRegistration();
}
