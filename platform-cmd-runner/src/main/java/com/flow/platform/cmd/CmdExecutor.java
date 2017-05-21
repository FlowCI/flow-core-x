package com.flow.platform.cmd;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gy@fir.im on 12/05/2017.
 *
 * @copyright fir.im
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

    public CmdExecutor(ProcListener procListener, LogListener logListener, String ...cmd) {
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

            System.out.println(" ===== Process executed =====");
            if (procListener != null) {
                procListener.onExecuted(outputResult);
            }

            waitLock.await(30, TimeUnit.SECONDS); // wait max 30 seconds
            System.out.println(" ===== Logging executed =====");

        } catch (InterruptedException | IOException e) {
            outputResult.getExceptions().add(e);

            if (procListener != null) {
                procListener.onException(outputResult);
            }
        } finally {
            // calculate duration
            outputResult.setFinishTime(new Date());

            if (procListener != null) {
                procListener.onFinished(outputResult);
            }
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
                }
            }
        };
    }

    private Runnable createCmdStreamReader(Process p) {
        return new CmdStreamReader(p.getInputStream(), bufferSize, new CmdStreamReader.CmdStreamListener() {
            @Override
            public void onLogging(String line) {
                loggingQueue.add(line);
                loggingQueueSize.getAndIncrement();
            }

            @Override
            public void onFinish() {
                isLoggingFinish.set(true);
            }
        });
    }
}
