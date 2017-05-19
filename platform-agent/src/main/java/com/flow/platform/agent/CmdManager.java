package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdManager {

    private static final CmdManager INSTANCE = new CmdManager();

    private final Set<CmdResult> running = Sets.newConcurrentHashSet();
    private final Queue<CmdResult> finished = new ConcurrentLinkedQueue<>();

    public static CmdManager getInstance() {
        return INSTANCE;
    }

    // default listener
    private CmdExecutor.ProcListener procEventHandler = new ProcEventHandler();

    // handle extra listeners
    private List<CmdExecutor.ProcListener> extraProcEventListeners = new ArrayList<>(5);

    private CmdManager() {
    }

    public List<CmdExecutor.ProcListener> getExtraProcEventListeners() {
        return extraProcEventListeners;
    }

    /**
     * Get running proc
     *
     * @return
     */
    public Collection<CmdResult> getRunning() {
        return running;
    }

    /**
     * Get finished proc
     *
     * @return
     */
    public Collection<CmdResult> getFinished() {
        return finished;
    }

    /**
     * Close all executing processes
     */
    public void shutdown() {
        kill();
    }

    public void execute(final ZkCmd cmd) {
        if (cmd.getType() == ZkCmd.Type.RUN_SHELL) {
            CmdExecutor executor = new CmdExecutor(procEventHandler, "/bin/bash", "-c", cmd.getCmd());
            executor.run();
        }

        // kill current running proc-es
        if (cmd.getType() == ZkCmd.Type.STOP) {
            kill();
        }
    }

    /**
     * Kill current running process
     */
    public void kill() {
        for (CmdResult r : running) {
            r.getProcess().destroy();

            // update finish time
            r.setExecutedTime(new Date());
            r.setFinishTime(new Date());
            r.setExitValue(CmdResult.EXIT_VALUE_FOR_STOP);

            // set CmdResult to finish
            finished.add(r);

            AgentLog.info(String.format("Kill process : %s", r.toString()));
        }
        running.clear();
    }

    private class ProcEventHandler implements CmdExecutor.ProcListener {

        @Override
        public void onStarted(CmdResult result) {
            running.add(result);
            for (CmdExecutor.ProcListener listener : extraProcEventListeners) {
                listener.onStarted(result);
            }
        }

        @Override
        public void onExecuted(CmdResult result) {
            for (CmdExecutor.ProcListener listener : extraProcEventListeners) {
                listener.onExecuted(result);
            }
        }

        @Override
        public void onFinished(CmdResult result) {
            running.remove(result);
            finished.add(result);
            for (CmdExecutor.ProcListener listener : extraProcEventListeners) {
                listener.onFinished(result);
            }
        }

        @Override
        public void onException(CmdResult result) {
            for (CmdExecutor.ProcListener listener : extraProcEventListeners) {
                listener.onException(result);
            }
        }
    }
}
