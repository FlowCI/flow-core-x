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
     * Zk root path /flow-agents/{zone}/{name}
     */
    private final static String ZK_ROOT = "/flow-agents";
    private final static Object STATUS_LOCKER = new Object();

    private ZooKeeper zk;
    private ZkEventListener zkEventListener;

    private String zone;
    private String name;

    private String zonePath;
    private String nodePath;
    private String nodePathBusy;

    public AgentService(String zkHost, int zkTimeout, String zone, String name) throws IOException {
        this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        this.zone = zone;
        this.name = name;

        this.zonePath = String.format("%s/%s", ZK_ROOT, this.zone);
        this.nodePath = String.format("%s/%s/%s", ZK_ROOT, this.zone, this.name);
        this.nodePathBusy = String.format("%s/%s/%s-busy", ZK_ROOT, this.zone, this.name);
    }

    public AgentService(String zkHost, int zkTimeout, String zone, String name, ZkEventListener listener) throws IOException {
        this(zkHost, zkTimeout, zone, name);
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
        AgentLog.info(event.toString());

        try {
            if (ZkEventHelper.isConnectToServer(event)) {
                onConnected(event);
            }

            if (ZkEventHelper.isDataChangedOnPath(event, nodePath)) {
                onDataChanged(event);
            }

            if (ZkEventHelper.isDeletedOnPath(event, nodePath)) {
                stop();
            }
        } catch (Throwable e) {
            AgentLog.err(e, "Unexpected error");
            stop();
        }
    }

    private void onConnected(WatchedEvent event) {
        String path = register();
        if (zkEventListener != null) {
            zkEventListener.onConnected(event, path);
        }
    }

    private void onDataChanged(WatchedEvent event) {
        ZkCmd cmd = null;

        try {
            byte[] rawData = ZkNodeHelper.getNodeData(zk, event.getPath(), null);
            cmd = parseCmd(rawData);

            if (cmd != null) {
                ZkNodeHelper.createEphemeralNode(zk, getNodePathBusy(), rawData);


                if (zkEventListener != null) {
                    zkEventListener.onDataChanged(event, cmd);
                }
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
        ZkNodeHelper.monitoringNode(zk, nodePath, this, 5);
        ZkNodeHelper.monitoringNode(zk, nodePathBusy, this, 5);

        String path = ZkNodeHelper.createEphemeralNode(zk, nodePath, "");
        return path;
    }

    private ZkCmd parseCmd(byte[] jsonRaw) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(new String(jsonRaw), ZkCmd.class);
        } catch (Throwable e) {
            AgentLog.err(e, "Invalid command from sender");
            return null;
        }
    }
}
