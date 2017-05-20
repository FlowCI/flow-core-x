package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.domain.Cmd;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdManagerTest {

    private CmdManager cmdManager = CmdManager.getInstance();

    private static String resourcePath;

    @BeforeClass
    public static void before() throws IOException {
        System.setProperty(Config.PROP_CONCURRENT_PROC, "2");
        System.setProperty(Config.PROP_IS_DEBUG, "true");

        ClassLoader classLoader = CmdManagerTest.class.getClassLoader();
        resourcePath = classLoader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + resourcePath);
    }

    @Test
    public void should_be_singleton() {
        assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void running_process_should_be_recorded() throws InterruptedException {
        // given:
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);

        assertEquals(2, Config.concurrentProcNum());

        Cmd cmd = new Cmd(Cmd.Type.RUN_SHELL, resourcePath);
        cmdManager.getExtraProcEventListeners().add(new CmdExecutor.ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startLatch.countDown();
            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onFinished(CmdResult result) {
                finishLatch.countDown();
            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // when: execute two command by thread
        new Thread(() -> cmdManager.execute(cmd)).start();
        new Thread(() -> cmdManager.execute(cmd)).start();
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
}
