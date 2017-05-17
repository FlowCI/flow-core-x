package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdManager {

    private static final CmdManager instance = new CmdManager();

    private static final Set<CmdResult> running = Sets.newConcurrentHashSet();
    private static final Queue<CmdResult> finished = new ConcurrentLinkedQueue<>();

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(Config.concurrentProcNum(), new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    // Make thread to Daemon thread, those threads exit while JVM exist
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

    public static CmdManager getInstance() {
        return instance;
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
        executor.shutdownNow();
    }

    public void execute(final ZkCmd cmd) {
        executor.execute(() -> {
            if (cmd.getType() == ZkCmd.Type.RUN_SHELL) {
                CmdExecutor executor = new CmdExecutor(procEventHandler, "/bin/bash", "-c", cmd.getCmd());
                executor.run();
            }

            // kill current running job
            if (cmd.getType() == ZkCmd.Type.STOP) {
                kill();
            }
        });
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
