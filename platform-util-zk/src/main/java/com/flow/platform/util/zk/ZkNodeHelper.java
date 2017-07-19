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
import com.flow.platform.util.zk.ZkException.WatchingException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * @author gy@fir.im
 */
public class ZkNodeHelper {

    public static String createEphemeralNode(ZooKeeper zk, String path, String data) {
        return createEphemeralNode(zk, path, data.getBytes());
    }

    public static String createEphemeralNode(ZooKeeper zk, String path, byte[] data) {
        try {
            return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException.NodeExistsException e) {
            return path;
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    public static String createNode(ZooKeeper zk, String path, String data) {
        return createNode(zk, path, data.getBytes());
    }

    public static String createNode(ZooKeeper zk, String path, byte[] data) {
        try {
            return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
            return path;
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    public static void deleteNode(ZooKeeper zk, String path) {
        try {
            Stat stat = zk.exists(path, false);
            if (stat != null) {
                zk.delete(path, stat.getVersion());
            }
        } catch (InterruptedException | KeeperException e) {
            throw checkException(e);
        }
    }

    public static Stat watchNode(ZooKeeper zk, String path, Watcher watcher, int retry) {
        try {
            return zk.exists(path, watcher);
        } catch (KeeperException | InterruptedException e) {
            if (retry <= 0) {
                throw new WatchingException(e, path);
            }

            try {
                Thread.sleep(1000);
                return watchNode(zk, path, watcher, retry - 1);
            } catch (InterruptedException ie) {
                throw checkException(ie);
            }
        }
    }

    public static void watchChildren(ZooKeeper zk, String parentPath, Watcher watcher, int retry) {
        try {
            zk.getChildren(parentPath, watcher);
        } catch (KeeperException | InterruptedException e) {
            if (retry <= 0) {
                throw new WatchingException(e, parentPath);
            }

            try {
                Thread.sleep(1000);
                watchChildren(zk, parentPath, watcher, retry - 1);
            } catch (InterruptedException ie) {
                throw checkException(ie);
            }
        }
    }

    /**
     * @return Stat or null if not exited
     */
    public static Stat exist(ZooKeeper zk, String path) {
        try {
            return zk.exists(path, false);
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    public static byte[] getNodeData(ZooKeeper zk, String path, Stat stat) {
        try {
            return zk.getData(path, false, stat);
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    public static void setNodeData(ZooKeeper zk, String path, String data) {
        try {
            Stat stat = zk.exists(path, false);
            zk.setData(path, data.getBytes(), stat.getVersion());
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    public static List<String> getChildrenNodes(ZooKeeper zk, String rootPath) {
        try {
            return zk.getChildren(rootPath, false);
        } catch (KeeperException | InterruptedException e) {
            throw checkException(e);
        }
    }

    private static RuntimeException checkException(Exception e) {
        if (e instanceof KeeperException) {
            KeeperException zkException = (KeeperException) e;

            if (zkException.code() == KeeperException.Code.NONODE) {
                return new NotExitException(e, zkException.getPath());
            }

            if (zkException.code() == KeeperException.Code.BADVERSION) {
                return new BadVersion(e);
            }
        }

        return new ZkException.ConnectionException(e);
    }
}
