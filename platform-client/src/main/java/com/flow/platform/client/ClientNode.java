package com.flow.platform.client;

import com.flow.platform.domain.ClientStatus;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkEventListener;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;


/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */

public class ClientNode implements Runnable, Watcher {

    /**
     * Zk root path /flow-nodes/{zone}/{machine}
     */
    private final static String ZK_ROOT = "/flow-nodes";

    private final static Object STATUS_LOCKER = new Object();

    private ZooKeeper zk;
    private ZkEventListener zkEventListener;

    private String zone;
    private String machine;

    public ClientNode(String zkHost, int zkTimeout, String zone, String machine) throws IOException {
        this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        this.zone = zone;
        this.machine = machine;
    }

    public ClientNode(String zkHost, int zkTimeout, String zone, String machine, ZkEventListener listener) throws IOException {
        this(zkHost, zkTimeout, zone, machine);
        this.zkEventListener = listener;
    }

    public void stop() {
        synchronized (STATUS_LOCKER) {
            STATUS_LOCKER.notifyAll();
        }
    }

    @Override
    public void run() {
        synchronized (STATUS_LOCKER) {
            try {
                STATUS_LOCKER.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (ZkEventHelper.isConnectToServer(event)) {
            String path = register();
            if (zkEventListener != null) {
                zkEventListener.onConnected(event, path);
            }
        }

        if (ZkEventHelper.isDataChanged(event)) {
            try {
                byte[] rawData = ZkNodeHelper.getNodeData(zk, event.getPath());
                if (zkEventListener != null) {
                    zkEventListener.onDataChanged(event, rawData);
                }
            } catch (Exception e) {
                System.out.println("Invalid cmd from server");
            } finally {
                ZkNodeHelper.monitoringNode(zk, event.getPath(), this, 5);
            }
        }

        if (ZkEventHelper.isDeleted(event)) {
            stop();
        }
    }


    /**
     * Register client node to server
     * Monitor data changed event
     *
     * @return path of zookeeper or null if failure
     */
    private String register() {
        String path = String.format("%s/%s/%s", ZK_ROOT, zone, machine);
        path = ZkNodeHelper.createEphemeralNode(zk, path, ClientStatus.IDLE.getName());
        ZkNodeHelper.monitoringNode(zk, path, this, 5);
        return path;
    }
}
