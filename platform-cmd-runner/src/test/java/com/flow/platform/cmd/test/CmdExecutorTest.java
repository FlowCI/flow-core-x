package com.flow.platform.cmd.test;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.CmdResult;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class CmdExecutorTest {

    @Test
    public void should_execute_command_with_correct_event() throws Throwable {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + path);

        LogListener logListener = new LogListener() {
            @Override
            public void onLog(Log log) {
                System.out.println(log);
            }

            @Override
            public void onFinish() {

            }
        };

        CmdExecutor executor = new CmdExecutor(null,
            logListener,
            null,
            null,
            "CMD_RUNNER_TEST",
            null,
            Lists.newArrayList(String.format("source %s", path)));
        CmdResult result = executor.run();

        Assert.assertNotNull(result.getStartTime());
        Assert.assertNotNull(result.getProcess());
        Assert.assertNotNull(result.getProcessId());

        Assert.assertNotNull(result.getExitValue());
        Assert.assertNotNull(result.getDuration());
        Assert.assertNotNull(result.getExecutedTime());
        Assert.assertEquals(1, result.getOutput().size());

        Assert.assertNotNull(result.getTotalDuration());
        Assert.assertNotNull(result.getFinishTime());
    }
}
