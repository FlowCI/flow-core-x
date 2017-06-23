package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.DateUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.*;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public final class CmdExecutor {

    /**
     * Class for null proc listener
     */
    private final class NullProcListener implements ProcListener {
        @Override
        public void onStarted(CmdResult result) {

        }

        @Override
        public void onLogged(CmdResult result) {

        }

        @Override
        public void onExecuted(CmdResult result) {

        }

        @Override
        public void onException(CmdResult result) {
        }
    }

    /**
     * Class for null log listener
     */
    private final class NullLogListener implements LogListener {
        @Override
        public void onLog(Log log) {

        }

        @Override
        public void onFinish() {

        }
    }

    private final static File DEFAULT_WORKING_DIR = new File(System.getProperty("user.home"));

    private final CountDownLatch logLath = new CountDownLatch(1);
    private final Queue<Log> loggingQueue = new LinkedList<>();
    private final AtomicInteger loggingQueueSize = new AtomicInteger(0);

    private final AtomicInteger stdCounter = new AtomicInteger(2); // std, stderr
    private final String endTerm = String.format("=====EOF-%s=====", UUID.randomUUID());
    private final int bufferSize = 1024 * 1024 * 5; // 5 mb buffer

    private ProcessBuilder pBuilder;
    private ProcListener procListener = new NullProcListener();
    private LogListener logListener = new NullLogListener();
    private Long timeout = new Long(3600 * 2); // process timeout in seconds, default is 2 hour
    private List<String> cmdList;
    private String outputEnvFilter;
    private CmdResult outputResult;

    /**
     * @param procListener    nullable
     * @param logListener     nullable
     * @param inputs          nullable input env
     * @param workingDir      nullable, for set cmd working directory, default is user.dir
     * @param outputEnvFilter nullable, for start_with env to cmd result output
     * @param cmd             exec cmd
     * @throws FileNotFoundException throw when working dir defined but not found
     */
    public CmdExecutor(final ProcListener procListener,
                       final LogListener logListener,
                       final Map<String, String> inputs,
                       final String workingDir,
                       final String outputEnvFilter,
                       final Long timeout,
                       final String... cmd) throws FileNotFoundException {

        if (procListener != null) {
            this.procListener = procListener;
        }

        if (logListener != null) {
            this.logListener = logListener;
        }

        this.outputEnvFilter = outputEnvFilter;
        cmdList = Lists.newArrayList(cmd);
        pBuilder = new ProcessBuilder("/bin/bash");

        // check and init working dir
        if (workingDir == null) {
            pBuilder.directory(DEFAULT_WORKING_DIR);
        } else {
            File dir = new File(workingDir);
            if (!dir.exists()) {
                throw new FileNotFoundException(String.format("Cmd defined working dir '%s' not found", workingDir));
            }
            pBuilder.directory(dir);
        }

        // init inputs env
        if (inputs != null && inputs.size() > 0) {
            pBuilder.environment().putAll(inputs);
        }

        // init timeout
        if (timeout != null) {
            this.timeout = timeout;
        }
    }

    public CmdResult run() {
        outputResult = new CmdResult();

        long startTime = System.currentTimeMillis();
        outputResult.setStartTime(ZonedDateTime.now());

        try {
            Process p = pBuilder.start();
            outputResult.setProcessId(getPid(p));
            outputResult.setProcess(p);

            procListener.onStarted(outputResult);

            Thread cmdRun = new Thread(createCmdListExec(p.getOutputStream(), cmdList));
            cmdRun.setDaemon(true);

            // thread to read stdout and stderr stream and enqueue
            Thread stdout = new Thread(createStdStreamReader(Log.Type.STDOUT, p.getInputStream()));
            stdout.setDaemon(true);


            Thread stderr = new Thread(createStdStreamReader(Log.Type.STDERR, p.getErrorStream()));
            stderr.setDaemon(true);

            // thread to make dequeue operation
            Thread logging = new Thread(createCmdLoggingReader());
            logging.setDaemon(true);

            cmdRun.start();
            stdout.start();
            stderr.start();
            logging.start();

            // wait for max process timeout
            if (p.waitFor(timeout.longValue(), TimeUnit.SECONDS)) {
                outputResult.setExitValue(p.exitValue());
            } else {
                outputResult.setExitValue(CmdResult.EXIT_VALUE_FOR_TIMEOUT);
            }

            outputResult.setExecutedTime(ZonedDateTime.now());
            System.out.println(String.format("====== 1. Process executed : %s ======", outputResult.getExitValue()));

            procListener.onExecuted(outputResult);

            logLath.await(30, TimeUnit.SECONDS); // wait max 30 seconds
            outputResult.setFinishTime(ZonedDateTime.now());

            procListener.onLogged(outputResult);

            System.out.println(String.format("====== 2. Logging executed : %s ======", logLath.getCount()));

        } catch (Throwable e) {
            outputResult.getExceptions().add(e);
            outputResult.setFinishTime(ZonedDateTime.now());

            procListener.onException(outputResult);
        } finally {
            System.out.println("====== 3. Process Done ======");
        }

        return outputResult;
    }

    /**
     * Get process id
     *
     * @param process
     * @return
     */
    private int getPid(Process process) {
        try {
            Class<?> cProcessImpl = process.getClass();
            Field fPid = cProcessImpl.getDeclaredField("pid");
            if (!fPid.isAccessible()) {
                fPid.setAccessible(true);
            }
            return fPid.getInt(process);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Make runnable to exec each cmd
     *
     * @param outputStream
     * @param cmdList
     * @return
     */
    private Runnable createCmdListExec(final OutputStream outputStream, final List<String> cmdList) {

        return new Runnable() {
            @Override
            public void run() {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                    for (String cmd : cmdList) {
                        writer.write(cmd + "\n");
                        writer.flush();
                    }

                    // find env and set to result output if output filter is not null or empty
                    if (!Strings.isNullOrEmpty(outputEnvFilter)) {
                        writer.write(String.format("echo %s\n", endTerm));
                        writer.write("env\n");
                        writer.flush();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Runnable createCmdLoggingReader() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (stdCounter.get() == 0 && loggingQueueSize.get() <= 0) {
                            break;
                        }

                        Log log = loggingQueue.poll();
                        if (log == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            logListener.onLog(log);
                            loggingQueueSize.getAndDecrement();
                        }
                    }
                } finally {
                    logListener.onFinish();
                    logLath.countDown();
                    System.out.println(" ===== Logging Reader Thread Finish =====");
                }
            }
        };
    }

    private Runnable createStdStreamReader(final Log.Type type, final InputStream is) {
        return new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is), bufferSize)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (Objects.equals(line, endTerm)) {
                            if (outputResult != null) {
                                readEnv(reader, outputResult.getOutput(), outputEnvFilter);
                            }
                            break;
                        }

                        loggingQueue.add(new Log(type, line));
                        loggingQueueSize.getAndIncrement();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    stdCounter.decrementAndGet();
                    System.out.println(String.format(" ===== %s Stream Reader Thread Finish =====", type));
                }
            }
        };
    }

    /**
     * Start when find log match 'endTerm', and load all env,
     * put env item which match 'start with filter' to CmdResult.output map
     *
     * @param reader
     * @param output
     * @param filter
     * @throws IOException
     */
    private void readEnv(final BufferedReader reader,
                         final Map<String, String> output,
                         final String filter) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(filter)) {
                int index = line.indexOf('=');
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                output.put(key, value);
            }
        }
    }
}
