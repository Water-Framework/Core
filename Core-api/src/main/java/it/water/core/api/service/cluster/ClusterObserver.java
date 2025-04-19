package it.water.core.api.service.cluster;

/**
 * Cluster members can subscribe to cluster events.
 *
 */
public interface ClusterObserver {
    /**
     * On cluster event all available informations are:
     * - event type
     * - node from which it has been generated
     * - data, custom array of bytes to support messages exchange
     * @param clusterEvent
     * @param nodeInfo
     * @param data
     */
    void onClusterEvent(ClusterEvent clusterEvent,ClusterNodeInfo nodeInfo,byte[] data);
}
