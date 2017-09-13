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

package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class NodeStatusTest {

    @Test(expected = IllegalParameterException.class)
    public void should_illegal_parameter_exception_if_null_cmd() {
        NodeStatus.transfer(null, false);
    }

    @Test(expected = IllegalParameterException.class)
    public void should_illegal_parameter_exception_if_no_cmd_status() {
        Cmd cmd = new Cmd();
        cmd.setStatus(null);
        NodeStatus.transfer(cmd, false);
    }

    @Test
    public void should_transfer_cmd_pending_or_sent_to_pending() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.PENDING);
        Assert.assertEquals(NodeStatus.PENDING, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.SENT);
        Assert.assertEquals(NodeStatus.PENDING, NodeStatus.transfer(cmd, false));
    }

    @Test
    public void should_transfer_cmd_running_or_executed_to_running() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.RUNNING);
        Assert.assertEquals(NodeStatus.RUNNING, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.EXECUTED);
        Assert.assertEquals(NodeStatus.RUNNING, NodeStatus.transfer(cmd, false));
    }

    @Test
    public void should_transfer_cmd_logged_to_success_or_failure() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setCmdResult(new CmdResult(0));
        Assert.assertEquals(NodeStatus.SUCCESS, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setCmdResult(new CmdResult(100));
        Assert.assertEquals(NodeStatus.FAILURE, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.LOGGED);
        Assert.assertEquals(NodeStatus.FAILURE, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setCmdResult(new CmdResult(100));
        Assert.assertEquals(NodeStatus.SUCCESS, NodeStatus.transfer(cmd, true));
    }

    @Test
    public void should_transfer_cmd_killed_or_exception_or_reject_to_failure() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.EXCEPTION);
        Assert.assertEquals(NodeStatus.FAILURE, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.KILLED);
        Assert.assertEquals(NodeStatus.FAILURE, NodeStatus.transfer(cmd, false));

        cmd = new Cmd();
        cmd.setStatus(CmdStatus.REJECTED);
        Assert.assertEquals(NodeStatus.FAILURE, NodeStatus.transfer(cmd, false));
    }

    @Test
    public void should_transfer_cmd_stopped_to_stopped() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.STOPPED);
        Assert.assertEquals(NodeStatus.STOPPED, NodeStatus.transfer(cmd, false));
    }

    @Test
    public void should_transfer_cmd_timeout_kill_to_timeout() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        Assert.assertEquals(NodeStatus.TIMEOUT, NodeStatus.transfer(cmd, false));
    }
}
