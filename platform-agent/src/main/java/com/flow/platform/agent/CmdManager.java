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

    private final ThreadFactory defaultFactory = r -> {
        // Make thread to Daemon thread, those threads exit while JVM exist
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    private final ThreadPoolExecutor cmdExecutor =
            new ThreadPoolExecutor(
                    Config.concurrentProcNum(),
                    Config.concurrentProcNum(),
                    0L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    defaultFactory,
                    (r, executor) -> AgentLog.info("Reach the max concurrent proc"));

    private final ExecutorService defaultExecutor =
            Executors.newFixedThreadPool(100, defaultFactory);

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
        cmdExecutor.shutdownNow();
    }

    public void execute(final ZkCmd cmd) {
        if (cmd.getType() == ZkCmd.Type.RUN_SHELL) {
            cmdExecutor.execute(() -> {
                CmdExecutor executor = new CmdExecutor(procEventHandler, "/bin/bash", "-c", cmd.getCmd());
                executor.run();
            });
        }

        // kill current running proc-es
        if (cmd.getType() == ZkCmd.Type.STOP) {
            defaultExecutor.execute(this::kill);
        }
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
