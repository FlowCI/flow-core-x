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

import com.flow.platform.api.domain.agent.AgentItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.service.AgentService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Jsonable;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class AgentServiceTest extends TestBase {

    @Autowired
    private AgentService agentService;

    private Integer jobNumber = 1;

    @Test
    public void should_list_agent_for_job() throws Throwable {
        // given: 3 jobs, 2 of them with running status
        final String sessionId = "123-456";

        List<Agent> mockAgentList = Lists.newArrayList(createMockAgent(sessionId));

        stubFor(get(urlEqualTo("/agents/list"))
            .willReturn(aResponse()
                .withBody(Jsonable.GSON_CONFIG.toJson(mockAgentList))));

        createMockJobWithResult(sessionId, NodeStatus.TIMEOUT);
        createMockJobWithResult(sessionId, NodeStatus.TIMEOUT);
        createMockJobWithResult(sessionId, NodeStatus.TIMEOUT);

        // when:
        List<AgentItem> list = agentService.listItems();
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());

        // then: no flow info since no job in running status
        Assert.assertNull(list.get(0).getFlowName());

        // when: set a job in running status
        createMockJobWithResult(sessionId, NodeStatus.RUNNING);
        list = agentService.listItems();
        Assert.assertNotNull(list);

        // then: should has flow info
        Assert.assertNotNull(list.get(0).getFlowName());
    }

    private Agent createMockAgent(String sessionId) {
        Agent agent = new Agent("zone", "name");
        agent.setStatus(AgentStatus.BUSY);
        agent.setSessionId(sessionId);
        return agent;
    }

    private Job createMockJobWithResult(String sessionId, NodeStatus status) {
        Job job = new Job(CommonUtil.randomId());
        job.setSessionId(sessionId);
        job.setNodePath("path");
        job.setNodeName("name");
        job.setNumber(jobNumber++);
        jobDao.save(job);

        NodeResult res = new NodeResult(job.getId(), "path");
        res.setStatus(status);
        res.setNodeTag(NodeTag.FLOW);
        res.setOrder(1);
        nodeResultDao.save(res);

        return job;
    }

}
