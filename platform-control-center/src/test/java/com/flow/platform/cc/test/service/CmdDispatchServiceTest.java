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

package com.flow.platform.cc.test.service;

import static org.junit.Assert.fail;

import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.event.NoAvailableResourceEvent;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdDispatchService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class CmdDispatchServiceTest extends TestBase {

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdDispatchService cmdDispatchService;

    @Autowired
    private List<Zone> defaultZones;

    private AgentPath agentPath;

    private Agent target;

    @Before
    public void toCreateSession() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-006";
        agentPath = new AgentPath(zoneName, agentName);

        zkClient.createEphemeral(ZKHelper.buildPath(agentPath), null);
        Thread.sleep(1000);

        // when: create cmd and dispatch to agent
        Cmd cmd = cmdService.create(new CmdInfo(zoneName, agentName, CmdType.CREATE_SESSION, null));
        Assert.assertNotNull(cmd.getSessionId());
        cmd = cmdDispatchService.dispatch(cmd.getId(), false);

        // then: check agent is locked by session
        target = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, target.getStatus());
        Assert.assertNotNull(target.getSessionId());
        Assert.assertEquals(cmd.getSessionId(), target.getSessionId());
    }

    @Test
    public void should_raise_exception_if_create_session_again() throws Throwable {
        Cmd cmdToFail = cmdService.create(new CmdInfo(agentPath, CmdType.CREATE_SESSION, null));

        // should throw agent not available exception
        try {
            cmdDispatchService.dispatch(cmdToFail.getId(), false);
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }
    }

    @Test
    public void should_send_cmd_with_session() throws Throwable {
        // when: send cmd with session id
        CmdInfo cmdWithSession = new CmdInfo(agentPath.getZone(), null, CmdType.RUN_SHELL, "echo hello");
        cmdWithSession.setSessionId(target.getSessionId());

        Cmd cmd = cmdService.create(cmdWithSession);
        cmd = cmdDispatchService.dispatch(cmd.getId(), false);

        // then: check target is found correctly by session id
        Agent sessionAgent = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(target, sessionAgent);
        Assert.assertEquals(target.getSessionId(), sessionAgent.getSessionId());
        Assert.assertEquals(AgentStatus.BUSY, sessionAgent.getStatus());
        Assert.assertNotNull(sessionAgent.getSessionDate());
    }

    @Test
    public void should_running_cmd_been_killed_when_delete_session() throws Throwable {
        // given:
        Cmd cmd = startRunShell(agentPath.getZone(), target.getSessionId());

        // then: mock cmd is running status
        CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.RUNNING, new CmdResult(), true, true);
        cmdService.updateStatus(statusItem, false);

        // when: delete session
        CmdInfo cmdToDelSession = new CmdInfo(agentPath.getZone(), null, CmdType.DELETE_SESSION, null);
        cmdToDelSession.setSessionId(target.getSessionId());
        cmdDispatchService.dispatch(cmdService.create(cmdToDelSession).getId(), false);

        // then: new kill cmd should been sent to agent
        Cmd killCmd = Cmd.parse(zkClient.getData(ZKHelper.buildPath(agentPath)), Cmd.class);
        Assert.assertNotNull(killCmd);
        Assert.assertEquals(CmdType.KILL, killCmd.getType());
        Assert.assertNotEquals(cmd.getId(), killCmd.getId());

        // then: verify agent status
        Agent sessionShouldReleased = agentService.find(cmd.getAgentPath());
        Assert.assertNull(sessionShouldReleased.getSessionId());
        Assert.assertEquals(AgentStatus.IDLE, sessionShouldReleased.getStatus());
    }

    @Test
    public void should_cmd_status_not_changed_for_finished_when_delete_session() throws Throwable {
        // given:
        Cmd cmd = startRunShell(agentPath.getZone(), target.getSessionId());

        // then: mock cmd is finished status
        CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.LOGGED, new CmdResult(), true, true);
        cmdService.updateStatus(statusItem, false);

        // when: delete session
        CmdInfo cmdToDelSession = new CmdInfo(agentPath.getZone(), null, CmdType.DELETE_SESSION, null);
        cmdToDelSession.setSessionId(cmd.getSessionId());
        cmdDispatchService.dispatch(cmdService.create(cmdToDelSession).getId(), false);

        // then: cmd in agent not changed
        Cmd notChangeCmd = Cmd.parse(zkClient.getData(ZKHelper.buildPath(agentPath)), Cmd.class);
        Assert.assertNotNull(notChangeCmd);
        Assert.assertEquals(CmdType.RUN_SHELL, notChangeCmd.getType());
        Assert.assertEquals(cmd.getId(), notChangeCmd.getId());

        // then: verify agent status
        Agent sessionShouldReleased = agentService.find(cmd.getAgentPath());
        Assert.assertNull(sessionShouldReleased.getSessionId());
        Assert.assertEquals(AgentStatus.IDLE, sessionShouldReleased.getStatus());
    }

    private Cmd startRunShell(String zone, String sessionId) {
        CmdInfo cmdWithSession = new CmdInfo(zone, null, CmdType.RUN_SHELL, "echo hello");
        cmdWithSession.setSessionId(sessionId);
        Cmd cmd = cmdService.create(cmdWithSession);
        return cmdDispatchService.dispatch(cmd.getId(), false);
    }
}
