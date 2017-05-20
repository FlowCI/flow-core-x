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
public class Agent {

    private static final String ZK_HOME = "54.222.129.38:2181";
    private static final int ZK_TIMEOUT = 2000;

    private static final String AGENT_ZONE = "firmac";
    private static final String AGENT_NAME = "test-001";

    public static void main(String args[]) throws IOException {
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

            System.out.println(zkHome);
            System.out.println(zone);
            System.out.println(name);
        }

        AgentLog.info("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        AgentService client = new AgentService(zkHome, ZK_TIMEOUT, zone, name, new ZkEventAdaptor() {

            @Override
            public void onConnected(WatchedEvent event, String path) {
                AgentLog.info("========= Agent connected to server =========");
            }

            @Override
            public void onDataChanged(WatchedEvent event, byte[] raw) {
                Cmd cmd = Cmd.parse(raw);
                AgentLog.info("Received command: " + cmd.toString());
                CmdManager.getInstance().execute(cmd);
            }

            @Override
            public void onDeleted(WatchedEvent event) {
                CmdManager.getInstance().shutdown(null);
                AgentLog.info("========= Agent been deleted =========");
            }
        });
        new Thread(client).start();
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            AgentLog.info("========= Agent end =========");
            AgentLog.info("JVM Exit");
        }
    }
}
