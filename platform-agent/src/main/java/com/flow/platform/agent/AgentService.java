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
import java.util.concurrent.*;

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

    private String zone; // agent running zone
    private String name; // agent name, can be machine name

    private String zonePath;    // zone path, /flow-agents/{zone}
    private String nodePath;    // zk node path, /flow-agents/{zone}/{name}
    private String nodePathBusy;// zk node path, /flow-agents/{zone}/{name}-busy

    private final ThreadFactory defaultFactory = r -> {
        // Make thread to Daemon thread, those threads exit while JVM exist
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    private final ThreadPoolExecutor cmdExecutor =
            new ThreadPoolExecutor(
                    Config.concurrentProcNum(),
                    Config.concurrentProcNum(),
                    0L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    defaultFactory,
                    (r, executor) -> AgentLog.info("Reach the max concurrent proc"));

    private final ExecutorService defaultExecutor =
            Executors.newFixedThreadPool(100, defaultFactory);

    public AgentService(String zkHost, int zkTimeout, String zone, String name) throws IOException {
        this.zk = new ZooKeeper(zkHost, zkTimeout, this);
        this.zone = zone;
        this.name = name;

        this.zonePath = String.format("%s/%s", ZK_ROOT, this.zone);
        this.nodePath = String.format("%s/%s/%s", ZK_ROOT, this.zone, this.name);
        this.nodePathBusy = String.format("%s/%s/%s-busy", ZK_ROOT, this.zone, this.name);
    }

    /**
     * Init AgentService with ZkEventListener
     *
     * @param zkHost
     * @param zkTimeout
     * @param zone
     * @param name
     * @param listener the onDataChanged of ZkEventListener is async, run on thread
     * @throws IOException
     */
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
                return;
            }

            if (ZkEventHelper.isDataChangedOnPath(event, nodePath)) {
                onDataChanged(event);
                return;
            }

            if (ZkEventHelper.isDeletedOnPath(event, nodePath)) {
                onDeleted(event);
            }
        } catch (Throwable e) {
            AgentLog.err(e, "Unexpected error");
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
        final ZkCmd cmd;

        try {
            final byte[] rawData = ZkNodeHelper.getNodeData(zk, nodePath, null);
            ZkNodeHelper.createEphemeralNode(zk, nodePathBusy, rawData);
            cmd = ZkCmd.parse(rawData);

            // fire onDataChanged in thread
            if (zkEventListener != null) {
                if (cmd.getType() == ZkCmd.Type.RUN_SHELL) {
                    ZkNodeHelper.createEphemeralNode(zk, nodePathBusy, rawData);

                    cmdExecutor.execute(() -> {
                        try {
                            zkEventListener.onDataChanged(event, cmd);
                        } finally {
                            ZkNodeHelper.deleteNode(zk, nodePathBusy);
                        }
                    });
                }

                else {
                    defaultExecutor.execute(() -> {
                        zkEventListener.onDataChanged(event, cmd);
                    });
                }
            }

        } catch (Throwable e) {
            AgentLog.err(e, "Invalid cmd from server");
        } finally {
            ZkNodeHelper.watchNode(zk, nodePath, this, 5);

            if (zkEventListener != null) {
                zkEventListener.afterOnDataChanged(event);
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
        String path = ZkNodeHelper.createEphemeralNode(zk, nodePath, "");
        ZkNodeHelper.watchNode(zk, nodePath, this, 5);
        return path;
    }
}