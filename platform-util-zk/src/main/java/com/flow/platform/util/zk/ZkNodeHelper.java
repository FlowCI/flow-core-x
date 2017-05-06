package com.flow.platform.util.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkNodeHelper {

    public static String createEphemeralNode(ZooKeeper zk, String path, String data) {
        try {
            return zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException.NodeExistsException e) {
            return path;
        } catch (KeeperException | InterruptedException e) {
            throw new ZkException.ZkServerConnectionException(e);
        }
    }

    public static Stat monitoringNode(ZooKeeper zk, String path, Watcher watcher, int retry) {
        try {
            return zk.exists(path, watcher);
        } catch (KeeperException | InterruptedException e) {

            if (retry <= 0) {
                throw new ZkException.ZkServerConnectionException(e);
            }

            try {
                Thread.sleep(1000);
                return monitoringNode(zk, path, watcher, retry - 1);
            } catch (InterruptedException ie) {
                throw new ZkException.ZkServerConnectionException(ie);
            }
        }
    }

    public static byte[] getNodeData(ZooKeeper zk, String path) {
        try {
            return zk.getData(path, false, null);
        } catch (KeeperException | InterruptedException e) {
            throw new ZkException.ZkServerConnectionException(e);
        }
    }
}
