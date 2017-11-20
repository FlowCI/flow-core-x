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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.service.node.NodeCrontabService;
import com.flow.platform.api.test.TestBase;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class NodeCrontabServiceTest extends TestBase {

    @Autowired
    private NodeCrontabService flowCrontabService;

    @Before
    public void before() {
        stubDemo();
        flowCrontabService.cleanTriggers();
    }

    @Test
    public void should_start_job_when_set_crontab_for_flow() throws Throwable {
        // given: flow and set crontab
        Node flow = createRootFlow("FirstFlow", "demo_flow.yaml");

        Map<String, String> envs = new HashMap<>();
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH.name(), "master");
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT.name(), "0/1 * * * ?");
        envService.save(flow, envs, true);

        Assert.assertNotNull(flow.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH));
        Assert.assertNotNull(flow.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT));

        // when: set crontab task for flow and wait for 70 seconds
        Thread.sleep(70 * 1000);

        // then: job should be created
        Assert.assertEquals(1, flowCrontabService.triggers().size());
        Job job = jobService.find(flow.getPath(), 1);
        Assert.assertEquals(JobCategory.SCHEDULER, job.getCategory());
        Assert.assertEquals("master", job.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH));
        Assert.assertEquals("0/1 * * * ?", job.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT));

        // when: delete env variable of crontab
        Set<String> varToDel = Sets.newHashSet(
            FlowEnvs.FLOW_TASK_CRONTAB_CONTENT.name(),
            FlowEnvs.FLOW_TASK_CRONTAB_BRANCH.name()
        );
        envService.delete(flow, varToDel, true);

        // then: check num of trigger
        Assert.assertEquals(0, flowCrontabService.triggers().size());
    }
}
