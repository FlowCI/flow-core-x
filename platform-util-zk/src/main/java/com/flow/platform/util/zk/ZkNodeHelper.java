package com.flow.platform.util.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
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
                throw new ZkException.ZkWatchingException(e, path);
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
                throw new ZkException.ZkWatchingException(e, parentPath);
            }

            try {
                Thread.sleep(1000);
                watchChildren(zk, parentPath, watcher, retry - 1);
            } catch (InterruptedException ie) {
                throw checkException(ie);
            }
        }
    }

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
                return new ZkException.ZkNoNodeException(e, zkException.getPath());
            }

            if (zkException.code() == KeeperException.Code.BADVERSION) {
                return new ZkException.ZkBadVersion(e);
            }
        }

        return new ZkException.ConnectionException(e);
    }
}
