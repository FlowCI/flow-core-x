package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.google.common.collect.Maps;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton class to handle command
 * <p>
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class CmdManager {

    private static final CmdManager INSTANCE = new CmdManager();

    public static CmdManager getInstance() {
        return INSTANCE;
    }

    // current running cmd data
    private final Map<Cmd, CmdResult> running = Maps.newConcurrentMap();

    // finished cmd data
    private final Map<Cmd, CmdResult> finished = Maps.newConcurrentMap();

    // rejected cmd data
    private final Map<Cmd, CmdResult> rejected = Maps.newConcurrentMap();

    // Make thread to Daemon thread, those threads exit while JVM exist
    private final ThreadFactory defaultFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    // Executor to execute command and shell
    private ThreadPoolExecutor cmdExecutor = createExecutor();

    // Executor to execute operations
    private ExecutorService defaultExecutor = Executors.newFixedThreadPool(100, defaultFactory);

    // handle extra listeners
    private List<ProcListener> extraProcEventListeners = new ArrayList<>(5);

    private CmdManager() {
    }

    /**
     * @return running cmd and result
     */
    public Map<Cmd, CmdResult> getRunning() {
        return running;
    }

    /**
     * @return finished cmd and result
     */
    public Map<Cmd, CmdResult> getFinished() {
        return finished;
    }

    /**
     * @return rejected cmd and result
     */
    public Map<Cmd, CmdResult> getRejected() {
        return rejected;
    }

    public List<ProcListener> getExtraProcEventListeners() {
        return extraProcEventListeners;
    }

    /**
     * Stop all executing processes and exit agent
     */
    public void stop() {
        kill();
        Runtime.getRuntime().exit(0);
    }

    public void shutdown(String password) {
        kill();
        try {
            if (password == null) {
                password = Config.sudoPassword(); // try to load sudo password from system property
            }

            if (password == null) {
                Logger.info("Shutdown cannot be executed since sudo password is null");
                return;
            }

            String shutdownCmd = String.format("echo %s | sudo -S shutdown -h now", password);
            Logger.info("Shutdown command: " + shutdownCmd);

            // exec shutdown command
            CmdExecutor executor = new CmdExecutor(null, null, "/bin/bash", "-c", shutdownCmd);
            executor.run();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute command from Cmd object by thread executor
     *
     * @param cmd Cmd object
     */
    public void execute(final Cmd cmd) {
        if (cmd.getType() == Cmd.Type.RUN_SHELL) {
            // check max concurrent proc
            int max = cmdExecutor.getMaximumPoolSize();
            int cur = cmdExecutor.getActiveCount();
            Logger.info(String.format(" ===== CmdExecutor: max=%s, current=%s =====", max, cur));

            // reach max proc number, reject this execute
            if (max == cur) {
                onReject(cmd);
                return;
            }

            cmdExecutor.execute(new TaskRunner(cmd) {
                @Override
                public void run() {
                    LogEventHandler logListener = new LogEventHandler(getCmd());
                    ProcEventHandler procEventHandler = new ProcEventHandler(getCmd(), extraProcEventListeners, running, finished);

                    CmdExecutor executor = new CmdExecutor(procEventHandler, logListener, "/bin/bash", "-c", getCmd().getCmd());
                    executor.run();
                }
            });

            return;
        }

        // kill current running proc
        if (cmd.getType() == Cmd.Type.KILL) {
            defaultExecutor.execute(this::kill);
            return;
        }

        // stop current agent
        if (cmd.getType() == Cmd.Type.STOP) {
            defaultExecutor.execute(this::stop);
            return;
        }

        if (cmd.getType() == Cmd.Type.SHUTDOWN) {
            defaultExecutor.execute(() -> {
                String passwordOfSudo = cmd.getCmd();
                shutdown(passwordOfSudo);
            });
        }
    }

    /**
     * Kill all current running process
     */
    public void kill() {
        for (Map.Entry<Cmd, CmdResult> entry : running.entrySet()) {
            CmdResult r = entry.getValue();

            r.getProcess().destroy();
            Logger.info(String.format("Kill process : %s", r.toString()));
        }

        try {
            cmdExecutor.shutdown(); // close all cmd thread
            cmdExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (Throwable e) {
            Logger.err(e, "Exception while waiting for all cmd thread finish");
        } finally {
            cmdExecutor = createExecutor();
            Logger.info("Cmd thread terminated");
        }
    }

    private void onReject(final Cmd cmd) {
        CmdResult rejectResult = new CmdResult();
        rejectResult.setExitValue(CmdResult.EXIT_VALUE_FOR_REJECT);

        Date now = new Date();
        rejectResult.setStartTime(now);
        rejectResult.setExecutedTime(now);
        rejectResult.setFinishTime(now);

        rejected.put(cmd, rejectResult);
        ReportManager.getInstance().cmdReportSync(cmd.getId(), Cmd.Status.REJECTED, null);
        Logger.warn(String.format("Reject cmd '%s' since over the limit proc of agent", cmd.getId()));
    }

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(
                Config.concurrentProcNum(),
                Config.concurrentProcNum(),
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                defaultFactory,
                (r, executor) -> {
                    if (r instanceof TaskRunner) {
                        TaskRunner task = (TaskRunner) r;
                        onReject(task.getCmd());
                        Logger.warn("Reject cmd: " + task.getCmd());
                    }
                });
    }

    private abstract class TaskRunner implements Runnable {

        private final Cmd cmd;

        public TaskRunner(Cmd cmd) {
            this.cmd = cmd;
        }

        public Cmd getCmd() {
            return cmd;
        }
    }
}
