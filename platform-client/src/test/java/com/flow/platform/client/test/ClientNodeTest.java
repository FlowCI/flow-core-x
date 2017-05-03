package com.flow.platform.client.test;

import com.flow.platform.client.ClientNode;
import com.flow.platform.domain.NodeStatus;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ClientNodeTest {

    private static final String ZK_HOST = "127.0.0.1:2181";

    private static ServerCnxnFactory zkFactory;
    private static ZooKeeper zkClient;

    @BeforeAll
    static void init() throws IOException, InterruptedException, KeeperException {
        int tickTime = 2000;
        int numConnections = 5000;
        String dataDirectory = System.getProperty("java.io.tmpdir");
        File dir = new File(dataDirectory, "zookeeper").getAbsoluteFile();
        ZooKeeperServer zkServer = new ZooKeeperServer(dir, dir, tickTime);

        zkFactory = ServerCnxnFactory.createFactory(2181, numConnections);
        zkFactory.getLocalPort();
        zkFactory.startup(zkServer);

        zkClient = new ZooKeeper(ZK_HOST, 2000, null);

        try {
            zkClient.create("/flow-nodes", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
        }

        try {
            zkClient.create("/flow-nodes/ali", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
        }
    }

    @Test
    void should_client_node_registered() throws IOException, KeeperException, InterruptedException {
        // init
        ClientNode node = new ClientNode(ZK_HOST, 2000, "ali", "f-cont-f11f827bd8af1");
        String path = node.register(null);

        // when
        byte[] data = zkClient.getData(path, false, null);
        NodeStatus status = NodeStatus.valueOf(new String(data));

        // then
        assertEquals(NodeStatus.IDLE, status);
    }

    @AfterAll
    static void done() {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }

}
