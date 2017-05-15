package com.flow.platform.agent.test;

import com.flow.platform.agent.AgentService;
import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkEventAdaptor;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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

        Path temp = Paths.get(System.getProperty("java.io.tmpdir"), "zookeeper");
        Files.walk(temp, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        ZooKeeperServer zkServer = new ZooKeeperServer(temp.toFile(), temp.toFile(), tickTime);

        zkFactory = ServerCnxnFactory.createFactory(2181, numConnections);
        zkFactory.getLocalPort();
        zkFactory.startup(zkServer);

        zkClient = new ZooKeeper(ZK_HOST, 20000, null);

        try {
            zkClient.create("/flow-agents", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
        }

        try {
            zkClient.create("/flow-agents/ali", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
        AgentService agent = new AgentService(ZK_HOST, 20000, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                assertEquals("/flow-agents/ali/" + MACHINE, path);

                try {
                    // when
                    byte[] data = ZkNodeHelper.getNodeData(zkClient, path, null);

                    // then
                    assertEquals("", new String(data));
                } finally {
                    waitState.countDown();
                }
            }
        });

        new Thread(agent).start();
        waitState.await();
    }

    @Test
    public void should_receive_command() throws InterruptedException, IOException {
        final CountDownLatch waitForConnect = new CountDownLatch(1);
        final CountDownLatch waitForCommandStart = new CountDownLatch(1);
        final CountDownLatch waitForBusyStatusRemoved = new CountDownLatch(1);

        AgentService client = new AgentService(ZK_HOST, 20000, ZONE, MACHINE, new ZkEventAdaptor() {
            @Override
            public void onConnected(WatchedEvent event, String path) {
                waitForConnect.countDown();
            }

            @Override
            public void onDataChanged(WatchedEvent event, ZkCmd cmd) {
                try {
                    // when
                    waitForCommandStart.countDown();

                    // then
                    assertEquals(new ZkCmd(ZkCmd.Type.RUN_SHELL, "~/test.sh"), cmd);

                    // simulate cmd running need 5 seconds
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    waitState.countDown();
                }
            }

            @Override
            public void afterOnDataChanged(WatchedEvent event, ZkCmd cmd) {
                waitForBusyStatusRemoved.countDown();
            }
        });

        new Thread(client).start();
        waitForConnect.await();

        // then: check node status after connected, should not busy
        Stat ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNull(ss);

        // when: send command to agent
        ZkCmd cmd = new ZkCmd();
        cmd.setType(ZkCmd.Type.RUN_SHELL);
        cmd.setCmd("~/test.sh");

        ZkNodeHelper.setNodeData(zkClient, client.getNodePath(), cmd.toJson());

        // then: check agent status when command received
        waitForCommandStart.await();
        ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNotNull(ss);

        // when: wait for command executed
        waitState.await();

        // then: check busy node should be deleted after command ran
        waitForBusyStatusRemoved.await();
        ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNull(ss);
    }

    @AfterClass
    public static void done() throws KeeperException, InterruptedException {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }

}
