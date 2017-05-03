package com.flow.platform.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * To receive zookeeper connection event
 *
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkConnectionWatcher implements Watcher {

    @Override
    public void process(WatchedEvent event) {
        if (!ZkEventHelper.isConnectToServer(event)) {

        }
    }
}
