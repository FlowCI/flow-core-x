package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdManager {

    private static final CmdManager instance = new CmdManager();

    private static final Set<CmdResult> running = Sets.newConcurrentHashSet();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    public static CmdManager getInstance() {
        return instance;
    }

    private CmdManager() {

    }

    /**
     * Close all executing processes
     */
    public void shutdown() {
        kill();
        executor.shutdownNow();
    }

    public void execute(ZkCmd cmd) {
        executor.execute(new RunCmd(cmd));
    }

    /**
     * Kill current running process
     */
    private void kill() {
        for (CmdResult r : running) {
            r.getProcess().destroy();
            AgentLog.info(String.format("Kill process : %s", r.toString()));
        }
        running.clear();
    }

    private class RunCmd implements Runnable {

        private CmdResult outResult;
        private ZkCmd zkCmd;

        RunCmd(ZkCmd zkCmd) {
            this.zkCmd = zkCmd;
            this.outResult = new CmdResult();
        }

        @Override
        public void run() {
            if (zkCmd.getType() == ZkCmd.Type.RUN_SHELL) {
                running.add(outResult);
                CmdExecutor executor = new CmdExecutor("/bin/bash", "-c", zkCmd.getCmd());
                executor.run(outResult);

                running.remove(outResult);
            }

            // kill current running job
            if (zkCmd.getType() == ZkCmd.Type.STOP) {
                kill();
            }
        }
    }
}
