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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.domain.AgentWithFlow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.NodeStatus;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "agentService")
public class AgentServiceImpl implements AgentService {

    private final Logger LOGGER = new Logger(AgentService.class);

    @Value(value = "${platform.agent.url}")
    private String agentUrl;

    @Value(value = "${platform.agent.shutdown.url}")
    private String shutdownAgentUrl;

    @Value(value = "${platform.zone}")
    private String zone;

    @Autowired
    private JobDao jobDao;

    @Override
    public List<AgentWithFlow> list() {
        StringBuilder stringBuilder = new StringBuilder(agentUrl);
        String res = HttpUtil.get(stringBuilder.toString());
        if (res == null) {
            throw new RuntimeException("Get Agent List error");
        }

        Agent[] agents = Jsonable.GSON_CONFIG.fromJson(res, Agent[].class);
        List<AgentWithFlow> agentWithFlows = new ArrayList<>();
        List<String> sessionIds = new ArrayList<>();
        for (Agent agent : agents) {
            if (agent.getSessionId() != null) {
                sessionIds.add(agent.getSessionId());
            }
        }
        List<Job> jobs = new ArrayList<>();
        if (!sessionIds.isEmpty()) {
            jobs = jobDao.list(sessionIds, NodeStatus.RUNNING);
        }
        LOGGER.trace(String.format("Job lenth %s", jobs.size()));

        for (Agent agent : agents) {
            Job job = matchJobBySessionId(jobs, agent.getSessionId());
            agentWithFlows.add(new AgentWithFlow(agent, job));
        }
        return agentWithFlows;
    }

    private Job matchJobBySessionId(List<Job> jobs, String sessionId) {
        Job j = null;
        for (Job job : jobs) {
            if (job.getSessionId().equals(sessionId)) {
                j = job;
                break;
            }
        }
        return j;
    }

    @Override
    public Boolean shutdown(String zone, String name, String password) {
        String url = new StringBuilder(shutdownAgentUrl).append("?zone=" + zone).append("&name=" + name)
            .append("&password=" + password).toString();

        try {
            String body = HttpUtil.post(url, "");
            Boolean res = Jsonable.GSON_CONFIG.fromJson(body, Boolean.class);
            return res;
        } catch (Throwable throwable) {
            LOGGER.traceMarker("shutdown", String.format("exception - %s", throwable));
            return false;
        }
    }
}
