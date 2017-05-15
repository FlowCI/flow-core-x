package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkEventAdaptor;
import com.google.common.collect.Sets;
import org.apache.zookeeper.WatchedEvent;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * To handle zk events
 * <p>
 * Created by gy@fir.im on 15/05/2017.
 *
 * @copyright fir.im
 */
public class AgentZkListener extends ZkEventAdaptor {

    private static final Set<CmdResult> running = Sets.newConcurrentHashSet();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public void onConnected(WatchedEvent event, String path) {
        AgentLog.info("========= Agent connected to server =========");
    }

    @Override
    public void onDataChanged(WatchedEvent event, ZkCmd cmd) {
        AgentLog.info("Received command: " + cmd.toString());

        // receive shell file path and execute
        executor.execute(new RunCmd(cmd));
    }

    @Override
    public void onDeleted(WatchedEvent event) {
        AgentLog.info("========= Agent been deleted =========");
    }

    private class RunCmd implements Runnable {

        private CmdResult outResult;
        private ZkCmd zkCmd;

        public RunCmd(ZkCmd zkCmd) {
            this.zkCmd = zkCmd;
            this.outResult = new CmdResult();
        }

        @Override
        public void run() {
            if (zkCmd.getType() == ZkCmd.Type.RUN_SHELL) {
                running.add(outResult);
                CmdExecutor executor = new CmdExecutor("/bin/bash", "-c", zkCmd.getCmd());
                executor.run(outResult);

                Process pid = outResult.getProcess();
                running.remove(outResult);
                AgentLog.info(String.format("Running pid is %s", pid));
            }

            // kill current running job
            if (zkCmd.getType() == ZkCmd.Type.STOP) {
                for (CmdResult r : running) {
                    r.getProcess().destroy();
                }
                running.clear();
            }
        }
    }

}
