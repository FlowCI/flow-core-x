package com.flow.platform.agent;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class Config {

    public final static String ZK_ROOT = "flow-agents";

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "flow.agent.debug";
    public final static String PROP_CONCURRENT_THREAD = "flow.agent.cmd.thread";
    public final static String PROP_REPORT_STATUS = "flow.agent.cmd.report";

    public final static String PROP_UPLOAD_AGENT_LOG = "flow.agent.log.upload";
    public final static String PROP_DEL_AGENT_LOG = "flow.agent.log.delete";
    public final static String PROP_LOG_DIR = "flow.agent.log.dir";

    public final static String PROP_ZK_TIMEOUT = "flow.agent.zk.timeout";
    public final static String PROP_SUDO_PASSWORD = "flow.agent.sudo.pwd";

    public static AgentConfig AGENT_CONFIG;
    public static String ZK_URL;
    public static String ZONE;
    public static String NAME;

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static int zkTimeout() {
        String intStr = System.getProperty(PROP_ZK_TIMEOUT, "10000"); // default 10 seconds
        return Integer.parseInt(intStr);
    }

    /**
     * Is delete cmd log after uploaded
     */
    public static boolean isDeleteLog() {
        String boolStr = System.getProperty(PROP_DEL_AGENT_LOG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static boolean isUploadLog() {
        String boolStr = System.getProperty(PROP_UPLOAD_AGENT_LOG, "true");
        return Boolean.parseBoolean(boolStr);
    }

    public static boolean isReportCmdStatus() {
        String boolStr = System.getProperty(PROP_REPORT_STATUS, "true");
        return Boolean.parseBoolean(boolStr);
    }

    public static Path logDir() {
        Path defaultPath = Paths.get(System.getenv("HOME"), "agent-log");
        String pathStr = System.getProperty(PROP_LOG_DIR, defaultPath.toString());

        try {
            return Paths.get(pathStr);
        } catch (Throwable e) {
            return defaultPath;
        }
    }

    public static int concurrentThreadNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_THREAD, "1");
        return Integer.parseInt(intStr);
    }

    public static String sudoPassword() {
        return System.getProperty(PROP_SUDO_PASSWORD, "");
    }

    public static AgentConfig agentConfig() {
        return AGENT_CONFIG;
    }

    /**
     * @return zone name
     */
    public static String zone() {
        return ZONE;
    }

    /**
     * @return agent name
     */
    public static String name() {
        return NAME;
    }

    public static String zkUrl() {
        return ZK_URL;
    }

    public static AgentConfig loadAgentConfig(String zkHost, int zkTimeout, String zoneName,
        int retry) throws IOException, InterruptedException {
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
            return Jsonable.parse(raw, AgentConfig.class);

        } catch (Throwable e) {
            if (retry > 0) {
                return loadAgentConfig(zkHost, zkTimeout, zoneName, retry - 1);
            }
            throw new RuntimeException(e);
        }
    }
}
