package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.cmd.CmdResult;
import com.flow.platform.util.zk.ZkCmd;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class CmdManagerTest {

    private CmdManager cmdManager = CmdManager.getInstance();

    @Test
    public void should_be_singleton() {
        assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void running_process_should_be_recorded() {
//        // given:
//        ClassLoader classLoader = this.getClass().getClassLoader();
//        String resourcePath = classLoader.getResource("test.sh").getFile();
//        ZkCmd cmd = new ZkCmd(ZkCmd.Type.RUN_SHELL, resourcePath);
//
//        // when:
//        cmdManager.execute(cmd);
//
//        // then:
//        Set<CmdResult> runningProc = cmdManager.getRunning();
//        assertEquals(0, runningProc.size());
//
//        Set<CmdResult> execProc = cmdManager.getExecuted();
//        assertEquals(1, execProc.size());
    }

    @Test
    public void running_process_should_be_killed() {

    }

}
