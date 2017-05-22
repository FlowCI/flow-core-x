package com.flow.platform.agent.test;

import com.flow.platform.agent.AgentService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.zk.ZkEventAdaptor;
import com.flow.platform.util.zk.ZkLocalBuilder;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
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

    private static ZooKeeper zkClient;
    private static ServerCnxnFactory zkFactory;

    @BeforeClass
    public static void init() throws IOException, InterruptedException, KeeperException {
        zkFactory = ZkLocalBuilder.start();
        zkClient = new ZooKeeper(ZK_HOST, 20000, null);

        ZkNodeHelper.createNode(zkClient, "/flow-agents", "");
        ZkNodeHelper.createNode(zkClient, "/flow-agents/ali", "");
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
            public void onDataChanged(WatchedEvent event, byte[] raw) {
                try {
                    // when
                    waitForCommandStart.countDown();
                    Cmd cmd = Cmd.parse(raw);

                    // then
                    assertEquals(new Cmd(ZONE, MACHINE, null, Cmd.Type.RUN_SHELL, "~/test.sh"), cmd);

                    // simulate cmd running need 5 seconds
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    waitState.countDown();
                }
            }

            @Override
            public void afterOnDataChanged(WatchedEvent event) {
                waitForBusyStatusRemoved.countDown();
            }
        });

        new Thread(client).start();
        waitForConnect.await();

        // then: check node status after connected, should not busy
        Stat ss = ZkNodeHelper.exist(zkClient, client.getNodePathBusy());
        assertNull(ss);

        // when: send command to agent
        Cmd cmd = new Cmd(ZONE, MACHINE, null, Cmd.Type.RUN_SHELL, "~/test.sh");
        ZkNodeHelper.setNodeData(zkClient, client.getNodePath(), cmd.toJson());

        // then: check agent status when command received
        waitForCommandStart.await();
        Thread.sleep(1000); // wait busy node

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
