package com.flow.platform.client;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkNodeHelper {

    static String createEphemeralNode(ZooKeeper zk, String path, String data) throws KeeperException, InterruptedException {
        try {
            return zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException.NodeExistsException e) {
            return path;
        }
    }
}
