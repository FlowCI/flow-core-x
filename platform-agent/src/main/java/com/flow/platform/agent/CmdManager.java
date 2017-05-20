package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
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
    private ProcListener procEventHandler = new ProcEventHandler();

    // handle extra listeners
    private List<ProcListener> extraProcEventListeners = new ArrayList<>(5);

    private CmdManager() {
    }

    public List<ProcListener> getExtraProcEventListeners() {
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
                AgentLog.info("Shutdown cannot be executed since sudo password is null");
                return;
            }

            String shutdownCmd = String.format("echo %s | sudo -S shutdown -h now", password);
            AgentLog.info("shutdown command: " + shutdownCmd);

            // exec shutdown command
            CmdExecutor executor = new CmdExecutor(procEventHandler, "/bin/bash", "-c", shutdownCmd);
            executor.run();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void execute(final Cmd cmd) {
        if (cmd.getType() == Cmd.Type.RUN_SHELL) {
            CmdExecutor executor = new CmdExecutor(procEventHandler, "/bin/bash", "-c", cmd.getCmd());
            executor.run();
            return;
        }

        // kill current running proc
        if (cmd.getType() == Cmd.Type.KILL) {
            kill();
            return;
        }

        // stop current agent
        if (cmd.getType() == Cmd.Type.STOP) {
            stop();
            return;
        }

        if (cmd.getType() == Cmd.Type.SHUTDOWN) {
            String passwordOfSudo = cmd.getCmd();
            shutdown(passwordOfSudo);
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

            //TODO: send cmd result to server here

            AgentLog.info(String.format("Kill process : %s", r.toString()));
        }
        running.clear();
    }

    private class ProcEventHandler implements ProcListener {

        @Override
        public void onStarted(CmdResult result) {
            running.add(result);
            for (ProcListener listener : extraProcEventListeners) {
                listener.onStarted(result);
            }
        }

        @Override
        public void onExecuted(CmdResult result) {
            for (ProcListener listener : extraProcEventListeners) {
                listener.onExecuted(result);
            }
        }

        @Override
        public void onFinished(CmdResult result) {
            running.remove(result);
            finished.add(result);
            for (ProcListener listener : extraProcEventListeners) {
                listener.onFinished(result);
            }
        }

        @Override
        public void onException(CmdResult result) {
            for (ProcListener listener : extraProcEventListeners) {
                listener.onException(result);
            }
        }
    }
}
