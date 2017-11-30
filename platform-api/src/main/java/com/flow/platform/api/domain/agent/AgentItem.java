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

package com.flow.platform.api.domain.agent;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;

/**
 * Class with flow and sync information
 *
 * @author yh@firim
 */
public class AgentItem extends Jsonable {

    @Expose
    private String name;

    @Expose
    private String zone;

    @Expose
    private String flowName;

    @Expose
    private String token;

    @Expose
    private AgentStatus agentStatus;

    @Expose
    private Integer number;

    @Expose
    private String zoneWithName;

    @Expose
    private String branch;

    @Expose
    private AgentSync sync;

    public AgentItem(AgentPath path, String flowName, AgentStatus agentStatus, Integer number) {
        this.name = path.getName();
        this.zone = path.getZone();
        this.flowName = flowName;
        this.agentStatus = agentStatus;
        this.number = number;
    }

    public AgentItem() {
    }

    public AgentItem(Agent agent, Job job) {
        this.name = agent.getPath().getName();
        this.zone = agent.getPath().getZone();
        this.agentStatus = agent.getStatus();
        this.token = agent.getToken();
        this.zoneWithName = this.name.concat(" - ").concat(this.zone);

        if (job != null) {
            this.flowName = job.getNodeName();
            this.number = job.getNumber();
            this.branch = "master";
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getZoneWithName() {
        return zoneWithName;
    }

    public void setZoneWithName(String zoneWithName) {
        this.zoneWithName = zoneWithName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
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

    public AgentSync getSync() {
        return sync;
    }

    public void setSync(AgentSync sync) {
        this.sync = sync;
    }
}
