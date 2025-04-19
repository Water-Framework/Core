package it.water.core.api.service.cluster;

/**
 * @Author Aristide Cittadino
 * Mapping cluster event types
 */
public enum ClusterEvent {
    /**
     * new peer has connected
     */
    PEER_CONNECTED,
    /**
     * A peer has disconnected
     */
    PEER_DISCONNECTED,
    /**
     * A peer is in an error state
     */
    PEER_ERROR,
    /**
     * Some informations about one peer has changed
     */
    PEER_INFO_CHANGED,
    /**
     * Peer want to comunicate custom data to other peers
     */
    PEER_DATA_EVENT,
    /**
     * Peer wants to raise a custom event for data exchange
     */
    PEER_CUSTOM_EVENT
}
