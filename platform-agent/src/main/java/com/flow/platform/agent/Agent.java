package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkEventAdaptor;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class Agent {

    private static final String ZK_HOME = "54.222.129.38:2181";
    private static final int ZK_TIMEOUT = 2000;

    private static final String NODE_ZONE = "firmac";
    private static final String NODE_MACHINE = "test-001";

    public static void main(String args[]) throws IOException {
        String zone;
        String machine;

        if (args.length != 2) {
            zone = NODE_ZONE;
            machine = NODE_MACHINE;
        } else {
            zone = args[0];
            machine = args[1];

            System.out.println(zone);
            System.out.println(machine);
        }

        AgentLog.info("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        AgentService client = new AgentService(ZK_HOME, ZK_TIMEOUT, zone, machine, new AgentZkListener());
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
