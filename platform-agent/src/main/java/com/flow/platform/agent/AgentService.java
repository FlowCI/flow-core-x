package com.flow.platform.agent;

import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkEventListener;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.gson.Gson;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;


/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */

public class AgentService implements Runnable, Watcher {

    /**
     * Zk root path /flow-nodes/{zone}/{machine}
     */
    private final static String ZK_ROOT = "/flow-nodes";

    private final static Object STATUS_LOCKER = new Object();

    private ZooKeeper zk;
    private ZkEventListener zkEventListener;

    private String zone;
    private String machine;

    private String zonePath;

    /**
     * ZK node path
     */
    private String nodePath;

    /**
     * ZK busy node path for agent status
     */
    private String nodePathBusy;

    public AgentService(String zkHost, int zkTimeout, String zone, String machine) throws IOException {
        this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        this.zone = zone;
        this.machine = machine;

        this.zonePath = String.format("%s/%s", ZK_ROOT, zone);
        this.nodePath = String.format("%s/%s/%s", ZK_ROOT, zone, machine);
        this.nodePathBusy = String.format("%s/%s/%s-busy", ZK_ROOT, zone, machine);
    }

    public AgentService(String zkHost, int zkTimeout, String zone, String machine, ZkEventListener listener) throws IOException {
        this(zkHost, zkTimeout, zone, machine);
        this.zkEventListener = listener;
    }

    public String getNodePath() {
        return nodePath;
    }

    public String getNodePathBusy() {
        return nodePathBusy;
    }

    /**
     * Stop agent
     */
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
        try {
            if (ZkEventHelper.isConnectToServer(event)) {
                String path = register();
                if (zkEventListener != null) {
                    zkEventListener.onConnected(event, path);
                }
            }

            if (ZkEventHelper.isDataChanged(event)) {
                ZkCmd cmd = null;

                try {
                    byte[] rawData = ZkNodeHelper.getNodeData(zk, event.getPath(), null);
                    ZkNodeHelper.createEphemeralNode(zk, getNodePathBusy(), rawData);
                    cmd = parseCmd(rawData);

                    if (zkEventListener != null) {
                        zkEventListener.onDataChanged(event, cmd);
                    }
                } catch (Throwable e) {
                    AgentLog.err(e, "Invalid cmd from server");
                } finally {
                    ZkNodeHelper.deleteNode(zk, getNodePathBusy());
                    ZkNodeHelper.monitoringNode(zk, event.getPath(), this, 5);

                    if (zkEventListener != null) {
                        zkEventListener.afterOnDataChanged(event, cmd);
                    }
                }
            }

            if (ZkEventHelper.isDeleted(event)) {
                stop();
            }
        } catch (Throwable e) {
            AgentLog.err(e, "Unexpected error");
            stop();
        }
    }

    /**
     * Register agent node to server
     * Monitor data changed event
     *
     * @return path of zookeeper or null if failure
     */
    private String register() {
        if (ZkNodeHelper.exist(zk, zonePath) == null) {
            ZkNodeHelper.createNode(zk, zonePath, "");
        }

        String path = ZkNodeHelper.createEphemeralNode(zk, nodePath, "");
        ZkNodeHelper.monitoringNode(zk, path, this, 5);
        return path;
    }

    private ZkCmd parseCmd(byte[] jsonRaw) {
        Gson gson = new Gson();
        return gson.fromJson(new String(jsonRaw), ZkCmd.class);
    }
}
