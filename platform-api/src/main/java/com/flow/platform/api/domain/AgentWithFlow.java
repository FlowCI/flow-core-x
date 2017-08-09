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

package com.flow.platform.api.domain;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;

/**
 * @author yh@firim
 */
public class AgentWithFlow extends Jsonable {

    @Expose
    private String agentName;

    @Expose
    private String agentZone;

    @Expose
    private String flowName;

    @Expose
    private AgentStatus agentStatus;

    @Expose
    private Integer number;

    @Expose
    private String branch;

    public AgentWithFlow(AgentPath path, String flowName, AgentStatus agentStatus, Integer number) {
        this.agentName = path.getName();
        this.agentZone = path.getZone();
        this.flowName = flowName;
        this.agentStatus = agentStatus;
        this.number = number;
    }

    public AgentWithFlow() {
    }

    public AgentWithFlow(Agent agent, Job job) {
        this.agentName = agent.getPath().getName();
        this.agentZone = agent.getPath().getZone();
        this.agentStatus = agent.getStatus();
        if (job != null) {
            this.flowName = job.getNodeName();
            this.number = 10;
            this.branch = "master";
        }
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentZone() {
        return agentZone;
    }

    public void setAgentZone(String agentZone) {
        this.agentZone = agentZone;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public AgentStatus getAgentStatus() {
        return agentStatus;
    }

    public void setAgentStatus(AgentStatus agentStatus) {
        this.agentStatus = agentStatus;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
