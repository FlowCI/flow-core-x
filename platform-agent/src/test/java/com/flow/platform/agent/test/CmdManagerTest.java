package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
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
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);

        ZkCmd cmd = new ZkCmd(ZkCmd.Type.RUN_SHELL, resourcePath);
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

        // when:
        cmdManager.execute(cmd);
        startLatch.await();

        // then:
        Collection<CmdResult> runningProc = cmdManager.getRunning();
        assertEquals(1, runningProc.size());

        // when:
        finishLatch.await();

        // then:
        Collection<CmdResult> execProc = cmdManager.getFinished();
        assertEquals(1, execProc.size());

        for (CmdResult r : execProc) {
            assertEquals(new Integer(0), r.getExitValue());
            assertEquals(true, r.getDuration() >= 2);
            assertEquals(0, r.getExceptions().size());
        }
    }
}
