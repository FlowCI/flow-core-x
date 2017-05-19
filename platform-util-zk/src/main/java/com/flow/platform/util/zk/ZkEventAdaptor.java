package com.flow.platform.util.zk;

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
    public void onDataChanged(WatchedEvent event, ZkCmd cmd) {

    }

    @Override
    public void onDeleted(WatchedEvent event) {

    }

    @Override
    public void afterOnDataChanged(WatchedEvent event) {

    }
}
