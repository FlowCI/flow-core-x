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
        CmdExecutor executor = new CmdExecutor("/bin/bash", "-c", "~/test.sh");
        CmdResult result = new CmdResult();
        executor.run(result);
        assert result.getProcess() != null;
        System.out.println(result);
    }
}
