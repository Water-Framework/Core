package it.water.core.service.cluster;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;

/**
 * @Author Aristide Cittadino
 * Cluster options.
 */
@FrameworkComponent
public class ClusterNodeOptionsImpl implements ClusterNodeOptions {

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Override
    public boolean clusterModeEnabled() {
        return Boolean.parseBoolean(applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_CLUSTER_MODE_ENABLED, "false"));
    }

    @Override
    public String getNodeId() {
        return applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_NODE_ID, "water-node-0");
    }

    @Override
    public String getLayer() {
        return applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_LAYER_ID, "microservices");
    }

    @Override
    public String getIp() {
        return applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_IP, "127.0.0.1");
    }

    @Override
    public String getHost() {
        return applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_HOST, "localhost");
    }

    @Override
    public boolean useIpInClusterRegistration() {
        return Boolean.parseBoolean(applicationProperties.getPropertyOrDefault(ClusterNodeOptions.PROP_USE_IP, "false"));
    }
}
