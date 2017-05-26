package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
public class CmdManagerTest {

    private CmdManager cmdManager = CmdManager.getInstance();

    private static String resourcePath;

    @BeforeClass
    public static void beforeClass() throws IOException {
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
    }

    @Test
    public void should_be_singleton() {
        assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void running_process_should_be_recorded() throws InterruptedException {
        // given:
        System.setProperty(Config.PROP_CONCURRENT_PROC, "2");

        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);

        assertEquals(2, Config.concurrentProcNum());

        Cmd cmd1 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd1.setId(UUID.randomUUID().toString());

        Cmd cmd2 = new Cmd("zone1", "agent2", Cmd.Type.RUN_SHELL, resourcePath);
        cmd2.setId(UUID.randomUUID().toString());

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

        // when: execute two command by thread
        new Thread(() -> cmdManager.execute(cmd1)).start();
        new Thread(() -> cmdManager.execute(cmd2)).start();
        startLatch.await();

        // then: check num of running proc
        Collection<CmdResult> runningProc = cmdManager.getRunning();
        assertEquals(2, runningProc.size());

        // when: wait two command been finished
        finishLatch.await();

        // then: check
        Collection<CmdResult> execProc = cmdManager.getFinished();
        assertEquals(2, execProc.size());

        for (CmdResult r : execProc) {
            assertEquals(new Integer(0), r.getExitValue());
            assertEquals(true, r.getDuration() >= 2);
            assertEquals(true, r.getTotalDuration() >= 2);
            assertEquals(0, r.getExceptions().size());
        }
    }

    @Test
    public void should_be_correct_status_for_killed_process() throws Throwable {
        // given
        Cmd cmd1 = new Cmd("zone1", "agent1", Cmd.Type.RUN_SHELL, resourcePath);
        cmd1.setId(UUID.randomUUID().toString());

        // when: start and kill task immediately
        new Thread(() -> cmdManager.execute(cmd1)).start();
        Thread.sleep(100);
        cmdManager.kill();

        // then: check CmdResult status
        Collection<CmdResult> finished = cmdManager.getFinished();
        Assert.assertEquals(1, finished.size());

        CmdResult result = (CmdResult) finished.toArray()[0];
        Assert.assertEquals(CmdResult.EXIT_VALUE_FOR_STOP, result.getExitValue());
    }
}
