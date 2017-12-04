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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class JobServiceConditionTest extends TestBase {

    @Before
    public void init() {
        stubDemo();

        stubFor(get(urlEqualTo("/mock-return-true"))
            .willReturn(aResponse()
                .withBody("true")));
    }

    @Test
    public void should_job_failure_since_condition_return_false() throws Throwable {
        // given: init flow and job
        String name = "flow-condition";
        Node rootForFlow = createRootFlow(name, "yml/condition.yml");
        Job job = createMockJob(rootForFlow.getPath());

        // when: mock create session callback, and condition should be executed
        final String sessionId = CommonUtil.randomId().toString();
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        job = reload(job);

        // then: node status should be stopped since step1 condition is return false
        NodeResult resultForStep1 = nodeResultService.find(PathUtil.build(name, "step1"), job.getId());
        Assert.assertEquals("Step 'step1' condition not match", resultForStep1.getFailureMessage());
        Assert.assertEquals(NodeStatus.STOPPED, resultForStep1.getStatus());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        // when: mock create step2 callback
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setCmdResult(new CmdResult(0));
        cmd.setExtra(PathUtil.build(name, "step2"));
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: step2 should be success
        NodeResult resultForStep2 = nodeResultService.find(PathUtil.build(name, "step2"), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForStep2.getStatus());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        // when: mock delete session callback
        cmd = new Cmd("default", null, CmdType.DELETE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then:
        job = reload(job);
        Assert.assertEquals(JobStatus.SUCCESS, job.getStatus());
    }
}
