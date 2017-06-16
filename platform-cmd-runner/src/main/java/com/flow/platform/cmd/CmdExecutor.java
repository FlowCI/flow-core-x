package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.DateUtil;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public final class CmdExecutor {

    private final static File DEFAULT_WORKING_DIR = new File(System.getProperty("user.home"));

    private final CountDownLatch logLath = new CountDownLatch(1);
    private final Queue<Log> loggingQueue = new LinkedList<>();
    private final AtomicInteger loggingQueueSize = new AtomicInteger(0);

    private final AtomicInteger stdCounter = new AtomicInteger(2); // std, stderr
    private final String endTerm = String.format("=====%s=====", UUID.randomUUID());
    private final int bufferSize = 1024 * 1024 * 5; // 5 mb buffer

    private ProcessBuilder pBuilder;
    private ProcListener procListener;
    private LogListener logListener;
    private Long timeout = new Long(3600 * 2); // process timeout in seconds, default is 2 hour

    /**
     * @param procListener nullable
     * @param logListener  nullable
     * @param inputs       nullable input env
     * @param workingDir   nullable, for set cmd working directory, default is user.dir
     * @param cmd          exec cmd
     * @throws FileNotFoundException throw when working dir defined but not found
     */
    public CmdExecutor(final ProcListener procListener,
                       final LogListener logListener,
                       final Map<String, String> inputs,
                       final String workingDir,
                       final Long timeout,
                       final String cmd) throws FileNotFoundException {
        this.procListener = procListener;
        this.logListener = logListener;

        pBuilder = new ProcessBuilder("/bin/bash", "-c", String.format("%s && echo %s", cmd, endTerm));

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

    public void run() {
        CmdResult outputResult = new CmdResult();

        long startTime = System.currentTimeMillis();
        outputResult.setStartTime(DateUtil.utcNow());

        try {
            Process p = pBuilder.start();
            outputResult.setProcessId(getPid(p));
            outputResult.setProcess(p);

            if (procListener != null) {
                procListener.onStarted(outputResult);
            }

            // thread to read stdout and stderr stream and enqueue
            Thread stdout = new Thread(createStdStreamReader(Log.Type.STDOUT, p.getInputStream()));
            Thread stderr = new Thread(createStdStreamReader(Log.Type.STDERR, p.getErrorStream()));

            // thread to make dequeue operation
            Thread logging = new Thread(createCmdLoggingReader());

            stdout.start();
            stderr.start();
            logging.start();

            // wait for max process timeout
            if (p.waitFor(timeout.longValue(), TimeUnit.SECONDS)) {
                outputResult.setExitValue(p.exitValue());
            } else {
                outputResult.setExitValue(CmdResult.EXIT_VALUE_FOR_TIMEOUT);
            }

            outputResult.setExecutedTime(DateUtil.utcNow());
            System.out.println(String.format("====== 1. Process executed : %s ======", outputResult.getExitValue()));

            if (procListener != null) {
                procListener.onExecuted(outputResult);
            }

            logLath.await(30, TimeUnit.SECONDS); // wait max 30 seconds
            outputResult.setFinishTime(DateUtil.utcNow());

            if (procListener != null) {
                procListener.onLogged(outputResult);
            }

            System.out.println(String.format("====== 2. Logging executed : %s ======", logLath.getCount()));

        } catch (Throwable e) {
            outputResult.getExceptions().add(e);
            outputResult.setFinishTime(DateUtil.utcNow());

            if (procListener != null) {
                procListener.onException(outputResult);
            }
        } finally {
            System.out.println("====== 3. Process Done ======");
        }
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
                            if (logListener != null) {
                                logListener.onLog(log);
                            }
                            loggingQueueSize.getAndDecrement();
                        }
                    }
                } finally {
                    if (logListener != null) {
                        logListener.onFinish();
                    }
                    logLath.countDown();
                    System.out.println(" ===== Logging Reader Thread Finish =====");
                }
            }
        };
    }

    private Runnable createStdStreamReader(final Log.Type type, final InputStream inputStream) {
        return new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), bufferSize)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
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
}
