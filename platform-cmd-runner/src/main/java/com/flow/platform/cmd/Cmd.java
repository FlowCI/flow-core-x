package com.flow.platform.cmd;

/**
 * Main entrance of cmd runner
 *
 * Created by gy@fir.im on 12/05/2017.
 *
 * @copyright fir.im
 */
public class Cmd {

    public static void main(String[] args) {

        CmdExecutor.ProcListener procListener = new CmdExecutor.ProcListener() {
            @Override
            public void onStarted(CmdResult result) {

            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onFinished(CmdResult result) {
                assert result.getProcess() != null;
                System.out.println(result);
            }

            @Override
            public void onException(CmdResult result) {

            }
        };

        CmdExecutor executor = new CmdExecutor(procListener, "/bin/bash", "-c", "~/test.sh");
        executor.run();
    }
}
