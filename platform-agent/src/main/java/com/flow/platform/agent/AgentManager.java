package com.flow.platform.agent;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */

public class AgentManager implements Runnable, Watcher {

    private final static Logger LOGGER = new Logger(AgentManager.class);

    // Zk root path /flow-agents/{zone}/{name}
    private final static Object STATUS_LOCKER = new Object();
    private final static int ZK_RECONNECT_TIME = 5;

    private String zkHost;
    private int zkTimeout;
    private ZooKeeper zk;
    private ZkEventListener zkEventListener;

    private String zone; // agent running zone
    private String name; // agent name, can be machine name

    private String zonePath;    // zone path, /flow-agents/{zone}
    private String nodePath;    // zk node path, /flow-agents/{zone}/{name}

    private final ReportManager reportManager = ReportManager.getInstance();

    public AgentManager(String zkHost, int zkTimeout, String zone, String name) throws IOException {
        this.zkHost = zkHost;
        this.zkTimeout = zkTimeout;

        this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        this.zone = zone;
        this.name = name;

        ZkPathBuilder pathBuilder = ZkPathBuilder.create(Config.ZK_ROOT).append(this.zone);
        this.zonePath = pathBuilder.path();
        pathBuilder.append(this.name);
        this.nodePath = pathBuilder.path();

        this.zkEventListener = new EventListener(); // using default event listener
    }

    /**
     * Init AgentService with ZkEventListener
     *
     * @param zkHost
     * @param zkTimeout
     * @param zone
     * @param name
     * @param listener  the onDataChanged of ZkEventListener is async, run on thread
     * @throws IOException
     */
    public AgentManager(String zkHost, int zkTimeout, String zone, String name, ZkEventListener listener) throws IOException {
        this(zkHost, zkTimeout, zone, name);
        this.zkEventListener = listener; // using input listener
    }

    public String getNodePath() {
        return nodePath;
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
        LOGGER.trace("Agent receive zookeeper event %s", event.toString());

        try {
            if (ZkEventHelper.isConnectToServer(event)) {
                onConnected(event);
                return;
            }

            if (ZkEventHelper.isDataChangedOnPath(event, nodePath)) {
                ZkNodeHelper.watchNode(zk, nodePath, this, 5);
                onDataChanged(event);
                return;
            }

            if (ZkEventHelper.isDeletedOnPath(event, nodePath)) {
                onDeleted(event);
            }

            if (ZkEventHelper.isSessionExpired(event)) {
                onReconnect(event);
            }
        } catch (Throwable e) {
            LOGGER.error("Unexpected error", e);

            // TODO: to handle zookeeper exception for reconnection, delete only temp solution
            onDeleted(event);
        }
    }

    /**
     * Force to exit current agent
     *
     * @param event
     */
    private void onDeleted(WatchedEvent event) {
        try {
            if (zkEventListener != null) {
                zkEventListener.onDeleted(event);
            }
            stop();
        } finally {
            Runtime.getRuntime().exit(1);
        }
    }

    private void onConnected(WatchedEvent event) {
        String path = register();
        if (zkEventListener != null) {
            zkEventListener.onConnected(event, path);
        }
    }

    private void onDataChanged(WatchedEvent event) {
        final Cmd cmd;

        try {
            final byte[] rawData = ZkNodeHelper.getNodeData(zk, nodePath, null);
            cmd = Jsonable.parse(rawData, Cmd.class);

            // fire onDataChanged in thread
            if (zkEventListener != null) {
                zkEventListener.onDataChanged(event, rawData);
            }

        } catch (Throwable e) {
            LOGGER.error("Invalid cmd from server", e);
        } finally {
            if (zkEventListener != null) {
                zkEventListener.afterOnDataChanged(event);
            }
        }
    }

    private void onReconnect(WatchedEvent event) {
        try {
            this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        } catch (IOException e) {
            LOGGER.error("Network failure while reconnect to zookeeper server", e);
        }
    }

    /**
     * Register agent node to server
     * Monitor data changed event
     *
     * @return path of zookeeper or null if failure
     */
    private String register() {
        String path = ZkNodeHelper.createEphemeralNode(zk, nodePath, "");
        ZkNodeHelper.watchNode(zk, nodePath, this, 5);
        return path;
    }

    /**
     * Class to handle customized zk event
     */
    private class EventListener extends ZkEventAdaptor {

        @Override
        public void onConnected(WatchedEvent event, String path) {
            LOGGER.trace("========= Agent connected to server =========");
        }

        @Override
        public void onDataChanged(WatchedEvent event, byte[] raw) {
            Cmd cmd = Jsonable.parse(raw, Cmd.class);
            LOGGER.trace("Received command: " + cmd.toString());
            CmdManager.getInstance().execute(cmd);
        }

        @Override
        public void onDeleted(WatchedEvent event) {
            CmdManager.getInstance().shutdown(null);
            LOGGER.trace("========= Agent been deleted =========");
        }
    }
}