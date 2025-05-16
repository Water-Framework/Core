package it.water.core.api.service.cluster;

import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;

import java.util.Collection;

/**
 * @Author Aristide Cittadino
 * Interface mapping the concept of Cluster Coordinator Client.
 * Every node network should have an implementation of this service which register the node itself to the cluster coordinator
 */
public interface ClusterCoordinatorClient extends Service {
    /**
     * When client is activated
     */
    @OnActivate
    void onActivate(ComponentRegistry componentRegistry);

    /**
     * When client is deactivated
     */
    @OnDeactivate
    void onDeactivate();

    /**
     *
     * @return true if the client has been initialized correctly
     */
    boolean isStarted();

    /**
     * Awaits for cluster connection
     * @throws InterruptedException
     */
    void awaitConnection() throws InterruptedException;

    /**
     * Registers current node into the cluster
     * @return
     */
    boolean registerToCluster();

    /**
     * Unregisters current node into the cluster
     * @return
     */
    boolean unregisterToCluster();

    /**
     * Checks wethere current node is leader of specificed taskId
     * @param leadershipServiceKey - key identifing the type of service running for leadership: ^[a-zA-Z0-9_-]+$
     * @return
     */
    boolean checkClusterLeadershipFor(String leadershipServiceKey);

    /**
     *
     * @param clusterNodeInfo
     * @return true if peer still exists
     */
    boolean peerStillExists(ClusterNodeInfo clusterNodeInfo);

    /**
     *
     * @return list of peers
     */
    Collection<ClusterNodeInfo> getPeerNodes();

    /**
     * Subscribes to cluster events
     * @param clusterObserver
     */
    void subscribeToClusterEvents(ClusterObserver clusterObserver);

    /**
     * Unsubscribe to cluster events
     * @param clusterObserver
     */
    void unsubscribeToClusterEvents(ClusterObserver clusterObserver);

    /**
     * Registers current node fot leadership on a specific leadershipServiceKey
     * @param
     * @param leadershipServiceKey
     */
    void registerForLeadership(String leadershipServiceKey);

    /**
     * Unregisters current node fot leadership on a specific leadershipServiceKey
     * @param leadershipServiceKey
     */
    void unregisterForLeadership(String leadershipServiceKey);
}
