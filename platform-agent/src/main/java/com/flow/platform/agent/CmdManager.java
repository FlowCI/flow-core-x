package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.google.common.collect.Sets;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class to handle command
 *
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
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
                Logger.info("Shutdown cannot be executed since sudo password is null");
                return;
            }

            String shutdownCmd = String.format("echo %s | sudo -S shutdown -h now", password);
            Logger.info("shutdown command: " + shutdownCmd);

            // exec shutdown command
            CmdExecutor executor = new CmdExecutor(procEventHandler, null, "/bin/bash", "-c", shutdownCmd);
            executor.run();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute command from Cmd object
     *
     * @param cmd Cmd object
     */
    public void execute(final Cmd cmd, final AgentConfig config) {
        if (cmd.getType() == Cmd.Type.RUN_SHELL) {
            LogEventHandler logListener = new LogEventHandler(cmd, config);
            CmdExecutor executor = new CmdExecutor(procEventHandler, logListener, "/bin/bash", "-c", cmd.getCmd());
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

            Logger.info(String.format("Kill process : %s", r.toString()));
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

    /**
     * To handle log event, upload log via socket io
     */
    private class LogEventHandler implements LogListener {

        private final static String SOCKET_EVENT_TYPE = "agent-logging";
        private final static int SOCKET_CONN_TIMEOUT = 10; // 10 seconds

        private final Cmd cmd;
        private final AgentConfig config;
        private final CountDownLatch initLatch = new CountDownLatch(1);

        private Socket socket;
        private boolean socketEnabled = false;

        private LogEventHandler(Cmd cmd, AgentConfig config) {
            this.cmd = cmd;
            this.config = config;

            if (this.config == null || this.config.getLoggingUrl() == null) {
                return;
            }

            try {
                socket = IO.socket(config.getLoggingUrl());
                socket.on(Socket.EVENT_CONNECT, objects -> initLatch.countDown());
                socket.connect();

                // wait socket connection and set socket enable to true
                initLatch.await(SOCKET_CONN_TIMEOUT, TimeUnit.SECONDS);
                if (initLatch.getCount() <= 0 && socket.connected()) {
                    socketEnabled = true;
                }

            } catch (URISyntaxException | InterruptedException e) {
                Logger.err(e, "Fail to connect socket io server: " + config.getLoggingUrl());
            }
        }

        @Override
        public void onLog(String log) {
            if (socketEnabled) {
                socket.emit(SOCKET_EVENT_TYPE, getLogFormat(log));
            }
            Logger.info(log);
        }

        @Override
        public void onFinish() {
            try {
                socket.close();
            } catch (Throwable e) {}
        }

        /**
         * Send log by format zone#agent#cmdId#log_raw_data
         *
         * @param log
         * @return
         */
        private String getLogFormat(String log) {
            return String.format("%s#%s#%s#%s", cmd.getZone(), cmd.getAgent(), cmd.getId(), log);
        }
    }
}
