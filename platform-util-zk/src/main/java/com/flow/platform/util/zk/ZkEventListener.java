package com.flow.platform.util.zk;

import org.apache.zookeeper.WatchedEvent;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public interface ZkEventListener {

    void onConnected(WatchedEvent event, String path);

    void onDataChanged(WatchedEvent event, byte[] data);

    void onDeleted(WatchedEvent event);
}
