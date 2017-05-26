package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class CmdManagerTest {

    private CmdManager cmdManager = CmdManager.getInstance();

    private static String resourcePath;

    @BeforeClass
    public static void beforeClass() throws IOException {
        System.setProperty(Config.PROP_CONCURRENT_PROC, "2");
        System.setProperty(Config.PROP_IS_DEBUG, "true");

        ClassLoader classLoader = CmdManagerTest.class.getClassLoader();
        resourcePath = classLoader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + resourcePath);
    }

    @Before
    public void beforeEach() {
        cmdManager.getExtraProcEventListeners().clear();
        cmdManager.getRunning().clear();
        cmdManager.getFinished().clear();
        cmdManager.getRejected().clear();
    }

    @Test
    public void should_be_singleton() {
        Assert.assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void running_process_should_be_recorded() throws InterruptedException {
        // given:
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);

        Assert.assertEquals(2, Config.concurrentProcNum());

        // create mock cmd
        Cmd cmd1 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd1.setId(UUID.randomUUID().toString());

        Cmd cmd2 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd2.setId(UUID.randomUUID().toString());

        Cmd cmd3 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd3.setId(UUID.randomUUID().toString());

        Cmd cmd4 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd4.setId(UUID.randomUUID().toString());

        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startLatch.countDown();
            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onLogged(CmdResult result) {
                finishLatch.countDown();
            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // when: execute four command by thread
        cmdManager.execute(cmd1);
        cmdManager.execute(cmd2);
        cmdManager.execute(cmd3);
        cmdManager.execute(cmd4);
        startLatch.await();

        // then: check num of running proc and reject cmd
        Map<Cmd, CmdResult> runningCmd = cmdManager.getRunning();
        Assert.assertEquals(2, runningCmd.size());
        Assert.assertTrue(runningCmd.containsKey(cmd1));
        Assert.assertTrue(runningCmd.containsKey(cmd2));

        Map<Cmd, CmdResult> rejectedCmd = cmdManager.getRejected();
        Assert.assertEquals(2, rejectedCmd.size());
        Assert.assertTrue(rejectedCmd.containsKey(cmd3));
        Assert.assertTrue(rejectedCmd.containsKey(cmd4));

        // when: wait two command been finished
        finishLatch.await();

        // then: check
        Map<Cmd, CmdResult> finishedCmd = cmdManager.getFinished();
        Assert.assertEquals(2, finishedCmd.size());

        for (Map.Entry<Cmd, CmdResult> entry : finishedCmd.entrySet()) {
            CmdResult r = entry.getValue();
            Assert.assertEquals(new Integer(0), r.getExitValue());
            Assert.assertEquals(true, r.getDuration() >= 2);
            Assert.assertEquals(true, r.getTotalDuration() >= 2);
            Assert.assertEquals(0, r.getExceptions().size());
        }
    }

    @Test
    public void should_be_correct_status_for_killed_process() throws Throwable {
        // given
        Cmd cmd = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd.setId(UUID.randomUUID().toString());

        // when: start and kill task immediately
        cmdManager.execute(cmd);
        Thread.sleep(100);
        cmdManager.kill();

        // then: check CmdResult status
        Map<Cmd, CmdResult> finished = cmdManager.getFinished();
        Assert.assertEquals(1, finished.size());

        CmdResult result = finished.get(cmd);
        Assert.assertNotNull(result.getDuration());
        Assert.assertNotNull(result.getExecutedTime());
        Assert.assertNotNull(result.getExitValue());
        Assert.assertEquals(CmdResult.EXIT_VALUE_FOR_STOP, result.getExitValue());
    }
}
