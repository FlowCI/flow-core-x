package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;

/**
 * Main entrance of cmd runner
 *
 * Created by gy@fir.im on 12/05/2017.
 *
 * @copyright fir.im
 */
public class App {

    public static void main(String[] args) {

        ProcListener procListener = new ProcListener() {
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

        LogListener logListener = new LogListener() {
            @Override
            public void onLog(String log) {
                System.out.println("Log: " + log);
            }

            @Override
            public void onFinish() {

            }
        };

        CmdExecutor executor = new CmdExecutor(procListener, logListener, "/bin/bash", "-c", "~/test.sh");
        executor.run();
    }
}
