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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class with flow and sync information
 *
 * @author yh@firim
 */
@NoArgsConstructor
public class AgentItem extends Jsonable {

    @Expose
    @Getter
    @Setter
    private String name;

    @Expose
    @Getter
    @Setter
    private String zone;

    @Expose
    @Getter
    @Setter
    private String flowName;

    @Expose
    @Getter
    @Setter
    private String token;

    @Expose
    @Getter
    @Setter
    private AgentStatus agentStatus;

    @Expose
    @Getter
    @Setter
    private Long number;

    @Expose
    @Getter
    @Setter
    private String zoneWithName;

    @Expose
    @Getter
    @Setter
    private String branch;

    @Expose
    @Getter
    @Setter
    private AgentSync sync;

    public AgentItem(AgentPath path, String flowName, AgentStatus agentStatus, Long number) {
        this.name = path.getName();
        this.zone = path.getZone();
        this.flowName = flowName;
        this.agentStatus = agentStatus;
        this.number = number;
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
}
