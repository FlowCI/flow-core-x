package com.flow.platform.agent.test;

import com.flow.platform.agent.AgentService;
import com.flow.platform.domain.ClientCommand;
import com.flow.platform.domain.ClientStatus;
import com.flow.platform.util.zk.ZkEventAdaptor;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class AgentTest {

    private static final String ZK_HOST = "127.0.0.1:2181";
    private static final String ZONE = "ali";
    private static final String MACHINE = "f-cont-f11f827bd8af1";

    private static ServerCnxnFactory zkFactory;
    private static ZooKeeper zkClient;

    @BeforeClass
    public static void init() throws IOException, InterruptedException, KeeperException {
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

    @Before
    public void beforeEach() {
        waitState = new CountDownLatch(1);
    }

    @Test
    public void should_agent_registered() throws IOException, KeeperException, InterruptedException {
        new AgentService(ZK_HOST, 2000g, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                assertEquals("/flow-nodes/ali/" + MACHINE, path);

                try {
                    // when
                    byte[] data = ZkNodeHelper.getNodeData(zkClient, path, null);
                    ClientStatus status = ClientStatus.valueOf(new String(data));

                    // then
                    assertEquals(ClientStatus.IDLE, status);
                } finally {
                    waitState.countDown();
                }
            }
        });

        waitState.await();
    }

    @Test
    public void should_receive_command() throws InterruptedException, IOException {
        final CountDownLatch waitForConnect = new CountDownLatch(1);
        final CountDownLatch waitForCommandStart = new CountDownLatch(1);

        AgentService client = new AgentService(ZK_HOST, 2000, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                waitForConnect.countDown();
            }

            @Override
            public void onDataChanged(WatchedEvent event, byte[] data) {
                try {
                    // when
                    waitForCommandStart.countDown();
                    ClientCommand command = ClientCommand.valueOf(new String(data));

                    // then
                    assertEquals(ClientCommand.RUN, command);

                    // simulate cmd running need 5 seconds
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    waitState.countDown();
                }
            }
        });

        waitForConnect.await();

        // then: check node status after connected, should not busy
        Stat ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNull(ss);

        // when: send command to agent
        ZkNodeHelper.setNodeData(zkClient, client.getNodePath(), "RUN");

        // then: check agent status when command received
        waitForCommandStart.await();
        ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNotNull(ss);

        // when: wait for command executed
        waitState.await();

        // then: check busy node should be deleted after command ran
        ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNull(ss);
    }

    @AfterClass
    public static void done() throws KeeperException, InterruptedException {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }

}
