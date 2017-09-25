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

import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.domain.AgentWithFlow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.service.job.CmdService;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.CollectionUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service
public class AgentServiceImpl implements AgentService {

    private final Logger LOGGER = new Logger(AgentService.class);

    @Value(value = "${platform.zone}")
    private String zone;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private PlatformURL platformURL;

    @Autowired
    private CmdService cmdService;

    @Override
    public List<AgentWithFlow> list() {
        String res = HttpUtil.get(platformURL.getAgentUrl());
        if (res == null) {
            throw new RuntimeException("Get Agent List error");
        }

        Agent[] agents = Jsonable.GSON_CONFIG.fromJson(res, Agent[].class);

        // get all session id from agent collection
        List<String> sessionIds = CollectionUtil.toPropertyList("sessionId", agents);

        // get all running jobs from agent sessions
        List<Job> jobs = new ArrayList<>(0);
        if (!CollectionUtil.isNullOrEmpty(sessionIds)) {
            jobs = jobDao.list(sessionIds, NodeStatus.RUNNING);
        }

        // convert to session - job map
        Map<String, Job> sessionJobMap = CollectionUtil.toPropertyMap("sessionId", jobs);
        if (CollectionUtil.isNullOrEmpty(sessionJobMap)) {
            sessionJobMap = new HashMap<>(0);
        }

        // build result list
        List<AgentWithFlow> agentWithFlows = new ArrayList<>(agents.length);
        for (Agent agent : agents) {
            String sessionIdFromAgent = agent.getSessionId();

            if (Strings.isNullOrEmpty(sessionIdFromAgent)) {
                agentWithFlows.add(new AgentWithFlow(agent, null));
                continue;
            }

            Job job = sessionJobMap.get(sessionIdFromAgent);
            if (job == null) {
                agentWithFlows.add(new AgentWithFlow(agent, null));
                continue;
            }

            agentWithFlows.add(new AgentWithFlow(agent, job));
        }
        return agentWithFlows;
    }

    @Override
    public Boolean shutdown(String zone, String name, String password) {
        try {
            cmdService.shutdown(new AgentPath(zone, name), password);
            return true;
        } catch (IllegalStatusException e) {
            LOGGER.warnMarker("shutdown", "Illegal shutdown state : " + e.getMessage());
            return false;
        }
    }

    @Override
    public Agent create(AgentPath agentPath) {
        try {
            String agentJson = HttpUtil.post(platformURL.getAgentCreateUrl(), agentPath.toJson());

            if (Strings.isNullOrEmpty(agentJson)) {
                throw new FlowException("Unable to create agent via control center");
            }

            return Agent.parse(agentJson, Agent.class);

        } catch (UnsupportedEncodingException | JsonSyntaxException e) {
            throw new IllegalStatusException("Unable to create agent", e);
        }
    }

    @Override
    public AgentSettings settings(String token) {
        String url = platformURL.getAgentSettingsUrl() + "?" + "token=" + token;
        String settingsJson = HttpUtil.get(url);

        if (Strings.isNullOrEmpty(settingsJson)) {
            throw new IllegalStatusException("Unable to get agent settings from control center");
        }

        return AgentSettings.parse(settingsJson, AgentSettings.class);
    }
}
