package com.flow.platform.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkEventHelper {

    /**
     * Client connect to server event
     *
     * @param event
     * @return
     */
    static boolean isConnectToServer(WatchedEvent event) {
        return syncConnectedEvent(event, Watcher.Event.EventType.None);
    }

    /**
     * Node deleted event
     *
     * @param event
     * @return
     */
    static boolean isDeleted(WatchedEvent event) {
        return syncConnectedEvent(event, Watcher.Event.EventType.NodeDeleted);
    }

    static boolean isDataChanged(WatchedEvent event) {
        return syncConnectedEvent(event, Watcher.Event.EventType.NodeDataChanged);
    }

    private static boolean syncConnectedEvent(WatchedEvent event, Watcher.Event.EventType eventType) {
        return event.getState() == Watcher.Event.KeeperState.SyncConnected && event.getType() == eventType;
    }
}
