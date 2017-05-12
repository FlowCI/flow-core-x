package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkEventListener;
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

    private static final String NODE_ZONE = "ali";
    private static final String NODE_MACHINE = "test-001";

    public static void main(String args[]) throws IOException {

        AgentLog.info("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        AgentService client = new AgentService(ZK_HOME, ZK_TIMEOUT, NODE_ZONE, NODE_MACHINE, new ZkListener());
        new Thread(client).run();

        AgentLog.info("========= Agent end =========");
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            AgentLog.info("JVM Exit");
        }
    }

    private static class ZkListener implements ZkEventListener {
        @Override
        public void onConnected(WatchedEvent event, String path) {
            AgentLog.info("========= Agent connected to server =========");
        }

        @Override
        public void onDataChanged(WatchedEvent event, byte[] data) {
            String shellFile = new String(data);
            AgentLog.info("Received command: " + shellFile);

            // receive shell file path and execute
            CmdResult cmdResult = new CmdResult();
            CmdExecutor executor = new CmdExecutor("/bin/bash", "-c", shellFile);
            executor.run(cmdResult);
            Integer pid = cmdResult.getPid();
            AgentLog.info(String.format("Running pid is %s", pid));
        }

        @Override
        public void onDeleted(WatchedEvent event) {
            AgentLog.info("========= Agent been deleted =========");
        }
    }
}
