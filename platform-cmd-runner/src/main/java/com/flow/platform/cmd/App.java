package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Main entrance of cmd runner
 * <p>
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public final class App {

    private final static ProcListener procListener = new ProcListener() {
        @Override
        public void onStarted(CmdResult result) {

        }

        @Override
        public void onExecuted(CmdResult result) {

        }

        @Override
        public void onLogged(CmdResult result) {
            assert result.getProcess() != null;
            System.out.println(result);
        }

        @Override
        public void onException(CmdResult result) {

        }
    };

    private final static LogListener logListener = new LogListener() {
        @Override
        public void onLog(Log log) {
            System.out.println(log);
        }

        @Override
        public void onFinish() {

        }
    };

    private final static ThreadFactory defaultFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(false);
        return t;
    };

    private final static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            defaultFactory,
            (r, executor) -> {
                System.out.println("Rejected !!!");
            });

    public static void main(String[] args) throws Throwable {
        Map<String, String> inputs = new HashMap<>();
        inputs.put("TEST", "echo \"hello\"");
        inputs.put("FLOW_INPUT", "HELLO");

        CmdExecutor executor = new CmdExecutor(
                procListener,
                logListener,
                inputs,
                null,
                "FLOW_", // find env start with FLOW_ and put to cmd result output map
                null,
                "$TEST", "echo $FLOW_INPUT", "echo $PWD", "export FLOW_TEST=112233", "cd ~/");

        executor.run();
    }

    private static class MyThread implements Runnable {
        @Override
        public void run() {
            try {
                CmdExecutor executor = new CmdExecutor(
                        procListener,
                        logListener,
                        null,
                        null,
                        null,
                        null,
                        "sleep 10 && echo \"hello\"");

                executor.run();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
