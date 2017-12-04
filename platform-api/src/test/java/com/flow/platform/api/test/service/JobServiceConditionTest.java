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

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.domain.Cmd;
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
    }

    @Test
    public void should_job_failure_since_condition_return_false() throws Throwable {
        // given: init flow and job
        String name = "flow-condition";
        Node rootForFlow = createRootFlow(name, "yml/condition_failure.yml");
        Job job = createMockJob(rootForFlow.getPath());

        // when: mock create session callback, and condition should be executed
        final String sessionId = CommonUtil.randomId().toString();
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        job = reload(job);

        // then: job should be stopped since step1 condition is return false
        NodeResult step1Result = nodeResultService.find("flow-condition/step1", job.getId());
        Assert.assertEquals("Step 'step1' condition not match", step1Result.getFailureMessage());
        Assert.assertEquals(NodeStatus.FAILURE, step1Result.getStatus());

        Assert.assertEquals("Step 'step1' condition not match", job.getFailureMessage());
        Assert.assertEquals(JobStatus.FAILURE, job.getStatus());
    }
}
