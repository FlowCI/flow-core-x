package com.flow.platform.util.zk;

import org.apache.zookeeper.WatchedEvent;

/**
 * Created by gy@fir.im on 06/05/2017.
 *
 * @copyright fir.im
 */
public class ZkEventAdaptor implements ZkEventListener {

    public void onConnected(WatchedEvent event, String path) {

    }

    public void onDataChanged(WatchedEvent event, byte[] data) {

    }

    public void onDeleted(WatchedEvent event) {

    }
}
