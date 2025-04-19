package it.water.core.api.service.cluster;

import java.util.Collection;

/**
 @Author Aristide Cittadino
  * Interface mapping the concept of Cluster Coordinator.
  * This is not mandatory since some cluster coordinator can used with custom technologies such as zookeeper.
  * If you want to have your own cluster coordinator simply implement this interface.
 */
public interface ClusterCoordinator {
    boolean addClusterSubscription(ClusterNodeInfo nodeInfo);
    boolean removeClusterSubscription(ClusterNodeInfo nodeInfo);
    boolean checkLeadershipFor(ClusterNodeInfo nodeInfo,String taskId);
    boolean peerExists(ClusterNodeInfo nodeInfo);
    Collection<ClusterNodeInfo> getPeerNodes();
}
