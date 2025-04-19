package it.water.core.api.service.cluster;

import it.water.core.api.service.Service;

import java.util.Collection;

/**
 * @Author Aristide Cittadino
 * Interface mapping the concept of Cluster Coordinator Client.
 * Every node network should have an implementation of this service which register the node itself to the cluster coordinator
 */
public interface ClusterCoordinatorClient extends Service {
    boolean registerToCluster();
    boolean unregisterToCluster();
    boolean checkClusterLeadershipFor(String taskId);
    boolean peerStillExists(ClusterNodeOptions clusterNodeOptions);
    Collection<ClusterNodeInfo> getPeerNodes(ClusterNodeOptions clusterNodeOptions);
    void subscribeToClusterEvents(ClusterObserver clusterObserver);
    void unsubscribeToClusterEvents(ClusterObserver clusterObserver);
}
