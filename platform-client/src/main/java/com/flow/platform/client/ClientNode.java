package com.flow.platform.client;

import com.flow.platform.domain.NodeStatus;
import org.apache.zookeeper.*;

import java.io.IOException;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */

public class ClientNode implements Runnable {

    /**
     * Zk root path /flow-nodes/{zone}/{machine}
     */
    private final static String ZK_ROOT = "/flow-nodes";

    private final static Object STATUS_LOCKER = new Object();

    private ZooKeeper zk;
    private String zone;
    private String machine;

    public ClientNode(String zkHost, int zkTimeout, String zone, String machine) throws IOException {
        this.zk = new ZooKeeper(zkHost, zkTimeout, new ZkConnectionWatcher());
        this.zone = zone;
        this.machine = machine;
    }

    /**
     * Register client node to server
     * Monitor data changed event
     *
     * @param watcher handle event of data change
     * @return path of zookeeper or null if failure
     */
    public String register(Watcher watcher) {
        String path = String.format("%s/%s/%s", ZK_ROOT, zone, machine);

        try {
            path = ZkNodeHelper.createEphemeralNode(zk, path, NodeStatus.IDLE.getName());
            zk.exists(path, watcher);
            return path;
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
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
}
