package com.flow.platform.cmd.test;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdExecutorTest {

    @Test
    public void should_execute_command_with_correct_event() {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test.sh").getFile();

        CmdExecutor executor = new CmdExecutor(new CmdExecutor.ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                Assert.assertNotNull(result.getStartTime());
                Assert.assertNotNull(result.getProcess());
            }

            @Override
            public void onExecuted(CmdResult result) {
                Assert.assertNotNull(result.getExitValue());
                Assert.assertNotNull(result.getDuration());
                Assert.assertNotNull(result.getExecutedTime());
            }

            @Override
            public void onFinished(CmdResult result) {
                Assert.assertNotNull(result.getTotalDuration());
                Assert.assertNotNull(result.getFinishTime());
            }

            @Override
            public void onException(CmdResult result) {
                System.out.println("onException");
            }
        }, "/bin/bash", "-c", path);

        executor.run();
    }
}
