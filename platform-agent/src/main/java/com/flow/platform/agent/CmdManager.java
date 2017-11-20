/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.agent;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.Log.Type;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class to handle command
 * <p>
 *
 * @author gy@fir.im
 */
public class CmdManager {

    private final static Logger LOGGER = new Logger(CmdManager.class);

    private final static CmdManager INSTANCE = new CmdManager();

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
    private ExecutorService defaultExecutor = Executors.newCachedThreadPool(defaultFactory);

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

    public ThreadPoolExecutor getCmdExecutor() {
        return cmdExecutor;
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
                LOGGER.trace("Shutdown cannot be executed since sudo password is null");
                return;
            }

            String shutdownCmd = String.format("echo %s | sudo -S shutdown -h now", password);
            LOGGER.trace("Shutdown command: " + shutdownCmd);

            // exec shutdown command
            CmdExecutor executor = new CmdExecutor(null, Lists.newArrayList(shutdownCmd));
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
        if (cmd.getType() == CmdType.RUN_SHELL) {
            // check max concurrent proc
            int max = cmdExecutor.getMaximumPoolSize();
            int cur = cmdExecutor.getActiveCount();
            LOGGER.trace(" ===== CmdExecutor: max=%s, current=%s =====", max, cur);

            // reach max proc number, reject this execute
            if (max == cur) {
                onReject(cmd);
                return;
            }

            cmdExecutor.execute(new TaskRunner(cmd) {
                @Override
                public void run() {
                    LOGGER.debug("start cmd ...");

                    LogEventHandler logListener = new LogEventHandler(getCmd());

                    ProcEventHandler procEventHandler =
                        new ProcEventHandler(getCmd(), extraProcEventListeners, running, finished);

                    try {
                        CmdExecutor executor = new CmdExecutor(
                            procEventHandler,
                            logListener,
                            cmd.getInputs(),
                            cmd.getWorkingDir(),
                            cmd.getOutputEnvFilter(),
                            cmd.getTimeout(),
                            Lists.newArrayList(getCmd().getCmd()));

                        executor.run();
                    } catch (Throwable e) {
                        LOGGER.errorMarker("execute", "Cannot init CmdExecutor for cmd " + cmd, e);
                        CmdResult result = new CmdResult();
                        result.getExceptions().add(e);
                        procEventHandler.onException(result);
                    }
                }
            });

            return;
        }

        if (cmd.getType() == CmdType.SYSTEM_INFO) {
            LogEventHandler logListener = new LogEventHandler(cmd);
            Log log = new Log(Type.STDERR, collectionAgentInfo());
            logListener.onLog(log);
            logListener.onFinish();
            cmd.setStatus(CmdStatus.EXECUTED);
            return;
        }

        // kill current running proc
        if (cmd.getType() == CmdType.KILL) {
            defaultExecutor.execute(this::kill);
            return;
        }

        // stop current agent
        if (cmd.getType() == CmdType.STOP) {
            defaultExecutor.execute(this::stop);
            return;
        }

        if (cmd.getType() == CmdType.SHUTDOWN) {
            defaultExecutor.execute(() -> {
                String passwordOfSudo = cmd.getCmd();
                shutdown(passwordOfSudo);
            });
        }
    }

    /**
     * collect agent info
     * @return
     */
    private String collectionAgentInfo() {
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long use = total - free;

        Map<String, String> dic = new HashMap<>(7);
        dic.put("javaVersion", javaVersion);
        dic.put("osName", osName);
        dic.put("totalMemory", Long.toString(total));
        dic.put("useMemory", Long.toString(use));
        dic.put("zone", Config.zone());
        dic.put("name", Config.name());
        dic.put("agentVersion", Config.getProperty("version"));

        return Jsonable.GSON_CONFIG.toJson(dic);
    }

    /**
     * Kill all current running process
     */
    public synchronized void kill() {
        cmdExecutor.shutdown();

        for (Map.Entry<Cmd, CmdResult> entry : running.entrySet()) {
            CmdResult r = entry.getValue();
            Cmd cmd = entry.getKey();
            finished.put(cmd, r);

            r.getProcess().destroy();

            ReportManager.getInstance().cmdReportSync(cmd.getId(), CmdStatus.KILLED, r);
            LOGGER.trace("Kill process : %s", r.toString());
        }

        try {
            cmdExecutor.shutdownNow();
        } catch (Throwable ignore) {

        } finally {
            cmdExecutor = createExecutor(); // reset cmd executor
            LOGGER.trace("Cmd thread terminated");
        }
    }

    private void onReject(final Cmd cmd) {
        CmdResult rejectResult = new CmdResult();
        rejectResult.setExitValue(CmdResult.EXIT_VALUE_FOR_REJECT);

        ZonedDateTime now = ZonedDateTime.now();
        rejectResult.setStartTime(now);
        rejectResult.setExecutedTime(now);
        rejectResult.setFinishTime(now);

        rejected.put(cmd, rejectResult);
        ReportManager.getInstance().cmdReportSync(cmd.getId(), CmdStatus.REJECTED, null);
        LOGGER.warn("Reject cmd '%s' since over the limit proc of agent", cmd.getId());
    }

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(
            Config.concurrentThreadNum(),
            Config.concurrentThreadNum(),
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            defaultFactory,
            (r, executor) -> {
                if (r instanceof TaskRunner) {
                    TaskRunner task = (TaskRunner) r;
                    onReject(task.getCmd());
                    LOGGER.warn("Reject cmd: %s", task.getCmd());
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
