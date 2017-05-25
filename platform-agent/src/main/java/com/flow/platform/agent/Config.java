package com.flow.platform.agent;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class Config {

    public final static String ZK_ROOT = "flow-agents";

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "flow.agent.debug";
    public final static String PROP_CONCURRENT_PROC = "flow.agent.concurrent";
    public final static String PROP_SUDO_PASSWORD = "flow.agent.sudo.pwd";

    private static AgentConfig AGENT_CONFIG;

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static int concurrentProcNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_PROC, "1");
        return Integer.parseInt(intStr);
    }

    public static String sudoPassword() {
        return System.getProperty(PROP_SUDO_PASSWORD, "");
    }

    public static AgentConfig agentConfig() {
        return AGENT_CONFIG;
    }

    public static void loadAgentConfig(String zkHost, int zkTimeout, String zoneName, int retry) throws IOException, InterruptedException {
        final CountDownLatch connectLatch = new CountDownLatch(1);

        try {
            // connect to zk server to load config from zone data
            ZooKeeper zkClient = new ZooKeeper(zkHost, zkTimeout, event -> {
                if (ZkEventHelper.isConnectToServer(event)) {
                    connectLatch.countDown();
                }
            });

            // wait 30 seconds to connect zk server
            if (!connectLatch.await(30, TimeUnit.SECONDS)) {
                throw new ZkException.ConnectionException(null);
            }

            String zonePath = ZkPathBuilder.create(ZK_ROOT).append(zoneName).path();
            byte[] raw = ZkNodeHelper.getNodeData(zkClient, zonePath, null);
            AGENT_CONFIG = AgentConfig.parse(raw);

        } catch (Throwable e) {
            if (retry > 0) {
                loadAgentConfig(zkHost, zkTimeout, zoneName, retry - 1);
                return;
            }
            throw new RuntimeException(e);
        }
    }
}
