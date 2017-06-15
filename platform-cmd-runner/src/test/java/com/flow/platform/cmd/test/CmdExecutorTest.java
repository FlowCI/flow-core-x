package com.flow.platform.cmd.test;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.CmdResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class CmdExecutorTest {

    @Test
    public void should_execute_command_with_correct_event() {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test.sh").getFile();

        ProcListener procListener = new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                Assert.assertNotNull(result.getStartTime());
                Assert.assertNotNull(result.getProcess());
                Assert.assertNotNull(result.getProcessId());
            }

            @Override
            public void onExecuted(CmdResult result) {
                Assert.assertNotNull(result.getExitValue());
                Assert.assertNotNull(result.getDuration());
                Assert.assertNotNull(result.getExecutedTime());
            }

            @Override
            public void onLogged(CmdResult result) {
                Assert.assertNotNull(result.getTotalDuration());
                Assert.assertNotNull(result.getFinishTime());
            }

            @Override
            public void onException(CmdResult result) {
                Assert.assertNotNull(result.getTotalDuration());
                Assert.assertNotNull(result.getFinishTime());
            }
        };

        LogListener logListener = new LogListener() {
            @Override
            public void onLog(Log log) {
                System.out.println(log);
            }

            @Override
            public void onFinish() {

            }
        };

        CmdExecutor executor = new CmdExecutor(procListener, logListener, "/bin/bash", "-c", path);
        executor.run();
    }
}
