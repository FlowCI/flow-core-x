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

package com.flow.platform.util.zk;

import com.flow.platform.util.zk.ZkException.BadVersion;
import com.flow.platform.util.zk.ZkException.NotExitException;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

/**
 * @author yang
 */
public class ZKClient implements Closeable {

    private final static int DEFAULT_RETRY_PERIOD = 1000;
    private final static int DEFAULT_RETRY_TIMES = 10;

    private CuratorFramework client;

    // async executor
    private Executor executor;

    private Map<String, NodeCache> nodeCaches = new ConcurrentHashMap<>();

    private Map<String, PathChildrenCache> nodeChildrenCache = new ConcurrentHashMap<>();

    private Map<String, TreeCache> nodeTreeCache = new ConcurrentHashMap<>();

    private int connTimeout = 30;

    public ZKClient(String host) {
        this(host, DEFAULT_RETRY_PERIOD, DEFAULT_RETRY_TIMES);
    }

    public ZKClient(String host, int connTimeout) {
        this(host, DEFAULT_RETRY_PERIOD, DEFAULT_RETRY_TIMES);
        this.connTimeout = connTimeout;
    }

    /**
     * @param host zookeeper host url
     * @param retryPeriod period between retry in millis
     * @param retryTimes num of retry
     */
    public ZKClient(String host, int retryPeriod, int retryTimes) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(retryPeriod, retryTimes);
        client = CuratorFrameworkFactory.newClient(host, retryPolicy);
    }

    public void setTaskExecutor(Executor executor) {
        this.executor = executor;
    }

    public boolean start() {
        try {
            client.start();

            if (client.blockUntilConnected(connTimeout, TimeUnit.SECONDS)) {
                return true;
            }

        } catch (InterruptedException ignore) {
        }
        return false;
    }

    public boolean exist(String path) {
        try {
            return client.checkExists().forPath(path) != null;
        } catch (Throwable e) {
            throw new ZkException(String.format("Cannot check existing for path: %s", path), e);
        }
    }

    /**
     * Create zookeeper node if not exist, or update node data
     *
     * @param path target zookeeper node path
     * @param data node data, it can be set to null
     * @return zookeeper node path just created
     */
    public String create(String path, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        if (exist(path)) {
            setData(path, data);
            return path;
        }

        try {
            return client.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(path, data);
        } catch (Throwable e) {
            throw checkException(String.format("Fail to create node: %s", path), e);
        }
    }

    /**
     * Create zookeeper ephemeral node if not exist, or update node data
     *
     * @param path target zookeeper node path
     * @param data node data, it can be set to null
     * @return zookeeper node path just created
     */
    public String createEphemeral(String path, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        if (exist(path)) {
            setData(path, data);
            return path;
        }

        try {
            return client.create()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(path, data);
        } catch (Throwable e) {
            throw checkException(String.format("Fail to create node: %s", path), e);
        }
    }

    public List<String> getChildren(String rootPath) {
        try {
            return client.getChildren().forPath(rootPath);
        } catch (Throwable e) {
            throw checkException(String.format("Fail to get children of node: %s", rootPath), e);
        }
    }

    public void setData(String path, byte[] data) {
        if (!exist(path)) {
            throw new ZkException("Zookeeper node path does not existed", null);
        }

        try {
            client.setData().forPath(path, data);
        } catch (Throwable e) {
            throw checkException(String.format("Fail to set data for node: %s", path), e);
        }
    }

    public byte[] getData(String path) {
        if (!exist(path)) {
            throw new ZkException("Zookeeper node path does not existed", null);
        }

        try {
            return client.getData().forPath(path);
        } catch (Throwable e) {
            throw checkException(String.format("Fail to get data for node: %s", path), e);
        }
    }

    public void delete(String path, boolean isDeleteChildren) {
        try {
            if (!exist(path)) {
                return;
            }

            DeleteBuilder builder = client.delete();

            if (isDeleteChildren) {
                builder.guaranteed().deletingChildrenIfNeeded().forPath(path);
            } else {
                builder.guaranteed().forPath(path);
            }
        } catch (Throwable e) {
            throw checkException(String.format("Fail to delete node of path: %s", path), e);
        }
    }

    public boolean watchNode(String path, NodeCacheListener listener) {
        if (!exist(path)) {
            return false; // node doesn't exist
        }

        NodeCache nc = nodeCaches.get(path);
        if (nc != null) {
            return false; // node been listened
        }

        try {
            nc = new NodeCache(client, path);
            nc.start();

            if (executor != null) {
                nc.getListenable().addListener(listener, executor);
            } else {
                nc.getListenable().addListener(listener);
            }

            nodeCaches.put(path, nc);
            return true;
        } catch (Throwable e) {
            throw checkException(String.format("Unable to watch node: %s", path), e);
        }
    }

    public boolean watchTree(String path, TreeCacheListener listener) {
        TreeCache tc = nodeTreeCache.get(path);
        if (tc != null) {
            return false; // node been listened
        }

        try {
            tc = TreeCache.newBuilder(client, path).build();
            tc.start();

            if (executor != null) {
                tc.getListenable().addListener(listener, executor);
            } else {
                tc.getListenable().addListener(listener);
            }

            nodeTreeCache.put(path, tc);
            return true;
        } catch (Throwable e) {
            throw checkException(String.format("Unable to watch tree for path: %s", path), e);
        }
    }

    public boolean watchChildren(String rootPath, PathChildrenCacheListener listener) {
        if (!exist(rootPath)) {
            return false;
        }

        PathChildrenCache pcc = nodeChildrenCache.get(rootPath);
        if (pcc != null) {
            return false;
        }

        try {
            pcc = new PathChildrenCache(client, rootPath, false);
            pcc.start();

            if (executor != null) {
                pcc.getListenable().addListener(listener, executor);
            } else {
                pcc.getListenable().addListener(listener);
            }

            nodeChildrenCache.put(rootPath, pcc);
            return true;
        } catch (Throwable e) {
            throw checkException(String.format("Unable to watch children for root: %s", rootPath), e);
        }
    }

    @Override
    public void close() throws IOException {
        if (client == null) {
            return;
        }

        // close all node cache
        for (Map.Entry<String, NodeCache> entry : nodeCaches.entrySet()) {
            NodeCache value = entry.getValue();
            value.close();
        }

        // close all children cache
        for (Map.Entry<String, PathChildrenCache> entry : nodeChildrenCache.entrySet()) {
            PathChildrenCache value = entry.getValue();
            value.close();
        }

        // close all tree cache
        for (Map.Entry<String, TreeCache> entry : nodeTreeCache.entrySet()) {
            TreeCache value = entry.getValue();
            value.close();
        }

        if (client.getState() == CuratorFrameworkState.STARTED) {
            client.close();
        }
    }

    private static ZkException checkException(String defaultMessage, Throwable e) {
        if (e instanceof KeeperException) {
            KeeperException zkException = (KeeperException) e;

            if (zkException.code() == KeeperException.Code.NONODE) {
                return new NotExitException(defaultMessage, e);
            }

            if (zkException.code() == KeeperException.Code.BADVERSION) {
                return new BadVersion(e);
            }
        }

        return new ZkException(defaultMessage, e);
    }
}
