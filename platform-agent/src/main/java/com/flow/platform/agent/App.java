package com.flow.platform.agent;

import com.flow.platform.util.Logger;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class App {

    private final static Logger LOGGER = new Logger(App.class);

    private final static String ZK_HOME = "54.222.129.38:2181";
    private final static String AGENT_ZONE = "firmac";
    private final static String AGENT_NAME = "test-001";

    public static void main(String args[]) {
        String zkHome; // zookeeper address
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

            LOGGER.info(zkHome);
            LOGGER.info(zone);
            LOGGER.info(name);
        }

        LOGGER.trace("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            LOGGER.trace("========= Init config =========");

            Config.AGENT_CONFIG = Config.loadAgentConfig(zkHome, Config.zkTimeout(), zone, 5);
            LOGGER.trace(" -- Agent Config: %s", Config.agentConfig());

            Config.ZK_URL = zkHome;
            LOGGER.trace(" -- Zookeeper Url: %s", Config.zkUrl());

            Config.ZONE = zone;
            LOGGER.trace(" -- Zone Name: %s", Config.zone());

            Config.NAME = name;
            LOGGER.trace(" -- Agent Name: %s", Config.name());

            LOGGER.trace("========= Config initialized =========");
        } catch (Throwable e) {
            LOGGER.error("Cannot load agent config from zone", e);
            Runtime.getRuntime().exit(1);
        }

        try {
            AgentManager client = new AgentManager(zkHome, Config.zkTimeout(), zone, name);
            new Thread(client).start();
        } catch (Throwable e) {
            LOGGER.error("Got exception when agent running", e);
            Runtime.getRuntime().exit(1);
        }
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            LOGGER.trace("========= Agent end =========");
            LOGGER.trace("========= JVM EXIT =========");
        }
    }
}
