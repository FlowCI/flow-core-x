package com.flow.platform.agent;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.zk.ZkEventAdaptor;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class App {

    private static final String ZK_HOME = "54.222.129.38:2181";
    private static final String AGENT_ZONE = "firmac";
    private static final String AGENT_NAME = "test-001";

    public static void main(String args[]) {
        String zkHome; // zookeeper addres
        String zone; // agent zone
        String name; // agent name

        if (args.length != 3) {
            zkHome = ZK_HOME;
            zone = AGENT_ZONE;
            name = AGENT_NAME;
        } else {
            zkHome = args[0];
            zone = args[1];
            name = args[2];

            Logger.info(zkHome);
            Logger.info(zone);
            Logger.info(name);
        }

        Logger.info("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            Logger.info("========= Init config =========");

            Config.AGENT_CONFIG = Config.loadAgentConfig(zkHome, Config.zkTimeout(), zone, 5);
            Logger.info(String.format(" -- Agent Config: %s", Config.agentConfig()));

            Config.ZK_URL = zkHome;
            Logger.info(String.format(" -- Zookeeper Url: %s", Config.zkUrl()));

            Config.ZONE = zone;
            Logger.info(String.format(" -- Zone Name: %s", Config.zone()));

            Config.NAME = name;
            Logger.info(String.format(" -- Agent Name: %s", Config.name()));

            Logger.info("========= Config initialized =========");
        } catch (Throwable e) {
            Logger.err(e, "Cannot load agent config from zone");
            Runtime.getRuntime().exit(1);
        }

        try {
            AgentManager client = new AgentManager(zkHome, Config.zkTimeout(), zone, name);
            new Thread(client).start();
        } catch (Throwable e) {
            Logger.err(e, "Got exception when agent running");
            Runtime.getRuntime().exit(1);
        }
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            Logger.info("========= Agent end =========");
            Logger.info("========= JVM EXIT =========");
        }
    }
}
