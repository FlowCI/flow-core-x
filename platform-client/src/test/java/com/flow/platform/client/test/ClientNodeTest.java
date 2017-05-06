package com.flow.platform.client.test;

import com.flow.platform.client.ClientNode;
import com.flow.platform.client.ZkEventAdaptor;
import com.flow.platform.client.ZkNodeHelper;
import com.flow.platform.domain.ClientCommand;
import com.flow.platform.domain.NodeStatus;
import org.apache.zookeeper.*;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ClientNodeTest {

    private static final String ZK_HOST = "127.0.0.1:2181";
    private static final String ZONE = "ali";
    private static final String MACHINE = "f-cont-f11f827bd8af1";

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

    private CountDownLatch waitState;

    @BeforeEach
    void beforeEach() {
        waitState = new CountDownLatch(1);
    }

    @Test
    void should_client_node_registered() throws IOException, KeeperException, InterruptedException {
        new ClientNode(ZK_HOST, 2000, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                assertEquals("/flow-nodes/ali/" + MACHINE, path);

                // when
                byte[] data = ZkNodeHelper.getNodeData(zkClient, path);
                NodeStatus status = NodeStatus.valueOf(new String(data));

                // then
                assertEquals(NodeStatus.IDLE, status);
                waitState.countDown();
            }
        });

        waitState.await();
    }

    @Test
    void should_receive_data_changed() throws IOException, InterruptedException, KeeperException {
        final CountDownLatch waitForConnect = new CountDownLatch(1);

        new ClientNode(ZK_HOST, 2000, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                waitForConnect.countDown();
            }

            @Override
            public void onDataChanged(WatchedEvent event, byte[] data) {
                // when
                ClientCommand command = ClientCommand.valueOf(new String(data));

                // then
                assertEquals(ClientCommand.RUN, command);
                waitState.countDown();
            }
        });

        waitForConnect.await();
        zkClient.setData(String.format("/flow-nodes/ali/%s", MACHINE), "RUN".getBytes(), 0);
        waitState.await();
    }

    @AfterAll
    static void done() {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }

}
