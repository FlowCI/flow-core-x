package com.flow.platform.util.zk;

import org.apache.zookeeper.WatchedEvent;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * Copyright fir.im
 */
public interface ZkEventListener {

    /**
     * On zk client connected to server
     *
     * @param event zk raw event
     * @param path zk node path
     */
    void onConnected(WatchedEvent event, String path);

    /**
     * On receive zk data changed
     *
     * @param event zk raw event
     * @param cmd ZkCmd obj
     */
    void onDataChanged(WatchedEvent event, ZkCmd cmd);

    /**
     * On data changed been executed
     *
     * @param event
     * @param cmd
     */
    void afterOnDataChanged(WatchedEvent event, ZkCmd cmd);

    /**
     * On node deleted
     *
     * @param event
     */
    void onDeleted(WatchedEvent event);
}
