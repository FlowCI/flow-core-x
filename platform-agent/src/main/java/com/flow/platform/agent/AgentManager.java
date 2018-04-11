/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.agent;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.zk.ZKClient;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.ZKPaths;

/**
 * @author gy@fir.im
 */
@Log4j2
public class AgentManager implements Runnable, TreeCacheListener, AutoCloseable {

    // Zk root path /flow-agents/{zone}/{name}
    private final static Object STATUS_LOCKER = new Object();

    private final static int ZK_RECONNECT_TIME = 1;
    private final static int ZK_RETRY_PERIOD = 500;

    private String zkHost;
    private int zkTimeout;
    private ZKClient zkClient;

    private String zone; // agent running zone
    private String name; // agent name, can be machine name

    private String zonePath;    // zone path, /flow-agents/{zone}
    private String nodePath;    // zk node path, /flow-agents/{zone}/{name}

    private List<Cmd> cmdHistory = new LinkedList<>();

    public AgentManager(String zkHost, int zkTimeout, String zone, String name) throws IOException {
        this.zkHost = zkHost;
        this.zkTimeout = zkTimeout;

        this.zkClient = new ZKClient(zkHost, ZK_RETRY_PERIOD, ZK_RECONNECT_TIME);
        this.zone = zone;
        this.name = name;
        this.zonePath = ZKPaths.makePath(Config.ZK_ROOT, this.zone);
        this.nodePath = ZKPaths.makePath(this.zonePath, this.name);
    }

    public ZKClient getZkClient() {
        return zkClient;
    }

    public String getNodePath() {
        return nodePath;
    }

    public List<Cmd> getCmdHistory() {
        return cmdHistory;
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
        // init zookeeper
        zkClient.start();

        registerZkNodeAndWatch();

        synchronized (STATUS_LOCKER) {
            try {
                STATUS_LOCKER.wait();
            } catch (InterruptedException e) {
                log.warn("InterruptedException : " + e.getMessage());
            }
        }
    }

    private void exit() {
        log.info("One Agent is running in other place. Please first to stop another agent, thx!");
        Runtime.getRuntime().exit(1);
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        ChildData eventData = event.getData();

        if (event.getType() == Type.CONNECTION_RECONNECTED) {
            log.trace("========= Reconnect =========");
            registerZkNodeAndWatch();
            return;
        }

        if (event.getType() == Type.CONNECTION_LOST) {
            log.trace("========= Lost =========");
            return;
        }

        if (event.getType() == Type.INITIALIZED) {
            log.trace("========= Initialized =========");
            return;
        }

        if (event.getType() == Type.NODE_ADDED) {
            log.trace("========= Node Added: {} =========", eventData.getPath());
            return;
        }

        if (event.getType() == Type.NODE_UPDATED) {
            log.trace("========= Node Updated: {} =========", eventData.getPath());
            onDataChanged(eventData.getPath());
            return;
        }

        if (event.getType() == Type.NODE_REMOVED) {
            log.trace("========= Node Removed: {} =========", eventData.getPath());
            close();
        }
    }

    @Override
    public void close() {
        removeZkNode();
        stop();
    }

    /**
     * Force to exit current agent
     */
    private void onDeleted() {
        try {
            CmdManager.getInstance().shutdown(null);
            log.trace("========= Agent been deleted =========");

            stop();
        } finally {
            Runtime.getRuntime().exit(1);
        }
    }

    private void onDataChanged(String path) {
        final Cmd cmd;

        try {
            final byte[] rawData = zkClient.getData(path);
            if (rawData == null) {
                log.warn("Zookeeper node data is null");
                return;
            }

            cmd = Jsonable.parse(rawData, Cmd.class);
            if (cmd == null) {
                log.warn("Unable to parse cmd from zk node: " + new String(rawData));
                return;
            }

            cmdHistory.add(cmd);
            log.trace("Received command: " + cmd.toString());
            CmdManager.getInstance().execute(cmd);

        } catch (Throwable e) {
            log.error("Invalid cmd from server", e);
            // TODO: should report agent status directly...
        }
    }

    /**
     * Register agent node to server
     * Monitor data changed event
     *
     * @return path of zookeeper or null if failure
     */
    private String registerZkNodeAndWatch() {
        String path = zkClient.createEphemeral(nodePath, null);
        zkClient.watchTree(path, this);
        return path;
    }

    private void removeZkNode() {
        zkClient.deleteWithoutGuaranteed(nodePath, false);
    }
}