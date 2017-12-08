/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class CmdManagerTest extends TestBase {

    private CmdManager cmdManager = CmdManager.getInstance();

    private static String resourcePath;

    @BeforeClass
    public static void beforeClass() throws IOException {
        System.setProperty(Config.PROP_CONCURRENT_THREAD, "2");
        System.setProperty(Config.PROP_IS_DEBUG, "true");
        System.setProperty(Config.PROP_UPLOAD_AGENT_LOG, "false");
        System.setProperty(Config.PROP_REPORT_STATUS, "false");

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
        cmdManager.kill();
    }

    @Test
    public void should_be_singleton() {
        Assert.assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void should_has_cmd_log() throws Throwable {
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, resourcePath);
        cmd.setId(UUID.randomUUID().toString());
        cmdManager.execute(cmd);

        ThreadPoolExecutor cmdExecutor = cmdManager.getCmdExecutor();
        cmdExecutor.shutdown();
        cmdExecutor.awaitTermination(60, TimeUnit.SECONDS);

//        Assert.assertTrue(Files.exists(Paths.get(TEMP_LOG_DIR.toString(), cmd.getId() + ".out.zip")));
//        Assert.assertTrue(Files.exists(Paths.get(TEMP_LOG_DIR.toString(), cmd.getId() + ".err.zip")));
    }

    @Test
    public void running_process_should_be_recorded() throws InterruptedException {
        // given:
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);
        Assert.assertEquals(2, Config.concurrentThreadNum());

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

        // create mock cmd
        String content = String.format("source %s", resourcePath);
        ImmutableList<String> envFilter = ImmutableList.of("FLOW_UT_OUTPUT_1", "FLOW_UT_OUTPUT_2");

        Cmd cmd1 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd1.setOutputEnvFilter(envFilter);
        cmd1.setId(UUID.randomUUID().toString());

        Cmd cmd2 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd2.setOutputEnvFilter(envFilter);
        cmd2.setId(UUID.randomUUID().toString());

        Cmd cmd3 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd3.setOutputEnvFilter(envFilter);
        cmd3.setId(UUID.randomUUID().toString());

        Cmd cmd4 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd4.setOutputEnvFilter(envFilter);
        cmd4.setId(UUID.randomUUID().toString());

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
            Assert.assertEquals(2, r.getOutput().size());
        }
    }

    @Test
    public void should_be_correct_status_for_killed_process() throws Throwable {
        // given
        String content = String.format("source %s", resourcePath);
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd.setId(UUID.randomUUID().toString());

        CountDownLatch startLatch = new CountDownLatch(1);
        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startLatch.countDown();
            }

            @Override
            public void onLogged(CmdResult result) {

            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // when: start and kill task immediately
        cmdManager.execute(cmd);
        startLatch.await(30, TimeUnit.SECONDS);

        cmdManager.kill();

        // then: check CmdResult status
        Map<Cmd, CmdResult> finished = cmdManager.getFinished();
        Assert.assertEquals(1, finished.size());

        CmdResult result = finished.get(cmd);
        Assert.assertNotNull(result.getDuration());
        Assert.assertNotNull(result.getExecutedTime());
        Assert.assertNotNull(result.getExitValue());
        Assert.assertEquals(CmdResult.EXIT_VALUE_FOR_KILL, result.getExitValue());
    }


    @Test
    public void should_success_run_sys_cmd() throws InterruptedException {
        String content = String.format("source %s", resourcePath);
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd.setId(UUID.randomUUID().toString());
        CountDownLatch finishCountDownLatch = new CountDownLatch(1);
        CountDownLatch startCountDownLatch = new CountDownLatch(1);

        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startCountDownLatch.countDown();
                try {
                    Thread.sleep(3000);
                } catch (Throwable e) {
                }
            }

            @Override
            public void onLogged(CmdResult result) {
                finishCountDownLatch.countDown();
            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // when: start and kill task immediately
        cmdManager.execute(cmd);

        startCountDownLatch.await();
        Assert.assertEquals(1, cmdManager.getRunning().size());

        Cmd cmdSys = new Cmd("zone1", "agent1", CmdType.SYSTEM_INFO, "");
        cmdSys.setId(UUID.randomUUID().toString());

        cmdManager.execute(cmdSys);
        Assert.assertEquals(CmdStatus.EXECUTED, cmdSys.getStatus());

        finishCountDownLatch.await();
        Assert.assertEquals(1, cmdManager.getFinished().size());

    }
}
