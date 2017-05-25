package com.flow.platform.agent;

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
    private static final int ZK_TIMEOUT = 2000;

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

            System.out.println(zkHome);
            System.out.println(zone);
            System.out.println(name);
        }

        Logger.info("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            Config.loadAgentConfig(zkHome, ZK_TIMEOUT, zone, 5);
        } catch (Throwable e) {
            Logger.err(e, "Cannot load agent config from zone");
            Runtime.getRuntime().exit(1);
        }

        try {
            AgentManager client = new AgentManager(zkHome, ZK_TIMEOUT, zone, name);
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
            Logger.info("JVM Exit");
        }
    }
}
