package com.flow.platform.client;

import org.apache.zookeeper.WatchedEvent;

/**
 * Created by gy@fir.im on 06/05/2017.
 *
 * @copyright fir.im
 */
public class ZkEventAdaptor implements ZkEventListener {

    @Override
    public void onConnected(WatchedEvent event, String path) {

    }

    @Override
    public void onDataChanged(WatchedEvent event, byte[] data) {

    }

    @Override
    public void onDeleted(WatchedEvent event) {

    }
}
