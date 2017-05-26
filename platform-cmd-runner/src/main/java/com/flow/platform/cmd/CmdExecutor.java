package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class CmdExecutor {

    private final CountDownLatch waitLock = new CountDownLatch(1);
    private final Queue<String> loggingQueue = new LinkedList<>();
    private final AtomicInteger loggingQueueSize = new AtomicInteger(0);
    private final AtomicBoolean isLoggingFinish = new AtomicBoolean(false);
    private final int bufferSize = 1024 * 1024 * 10;

    private ProcessBuilder pBuilder;
    private ProcListener procListener;
    private LogListener logListener;

    /**
     * @param procListener nullable
     * @param logListener  nullable
     * @param cmd
     */
    public CmdExecutor(ProcListener procListener, LogListener logListener, String... cmd) {
        this.procListener = procListener;
        this.logListener = logListener;

        pBuilder = new ProcessBuilder(cmd);
        pBuilder.redirectErrorStream(true);
    }

    public void run() {
        CmdResult outputResult = new CmdResult();

        long startTime = System.currentTimeMillis();
        outputResult.setStartTime(new Date());

        try {
            Process p = pBuilder.start();
            outputResult.setProcessId(getPid(p));
            outputResult.setProcess(p);

            if (procListener != null) {
                procListener.onStarted(outputResult);
            }

            // start thread to read logging stream
            Thread threadForStream = new Thread(createCmdStreamReader(p));

            // start thread to make dequeue operation
            Thread threadForLogging = new Thread(createCmdLoggingReader());

            threadForStream.start();
            threadForLogging.start();

            outputResult.setExitValue(p.waitFor());
            outputResult.setExecutedTime(new Date());

            System.out.println(String.format("====== 1. Process executed : %s ======", outputResult.getExitValue()));
            if (procListener != null) {
                procListener.onExecuted(outputResult);
            }

            waitLock.await(30, TimeUnit.SECONDS); // wait max 30 seconds
            outputResult.setFinishTime(new Date());

            if (procListener != null) {
                procListener.onLogged(outputResult);
            }

            System.out.println(String.format("====== 2. Logging executed : %s ======", waitLock.getCount()));

        } catch (InterruptedException | IOException e) {
            outputResult.getExceptions().add(e);
            outputResult.setFinishTime(new Date());

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
                        if (isLoggingFinish.get() && loggingQueueSize.get() <= 0) {
                            break;
                        }

                        String line = loggingQueue.poll();
                        if (line == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            if (logListener != null) {
                                logListener.onLog(line);
                            }
                            loggingQueueSize.getAndDecrement();
                        }
                    }
                } finally {
                    if (logListener != null) {
                        logListener.onFinish();
                    }
                    waitLock.countDown();
                    System.out.println(" ===== Logging Reader Thread Finish =====");
                }
            }
        };
    }

    private Runnable createCmdStreamReader(final Process p) {
        return new Runnable() {
            @Override
            public void run() {
                isLoggingFinish.set(false);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()), bufferSize)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        loggingQueue.add(line);
                        loggingQueueSize.getAndIncrement();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    isLoggingFinish.set(true);
                    System.out.println(" ===== Stream Reader Thread Finish =====");
                }
            }
        };
    }
}
