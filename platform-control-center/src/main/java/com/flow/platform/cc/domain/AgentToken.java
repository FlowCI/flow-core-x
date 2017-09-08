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

package com.flow.platform.cc.domain;

import com.flow.platform.domain.AgentPath;

/**
 * @author yh@firim
 */
public class AgentToken {

    private AgentPath agentPath;
    private String token;
    private Integer ts;

    public AgentToken(AgentPath agentPath, String token) {
        this.agentPath = agentPath;
        this.token = token;
    }


    public AgentPath getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(AgentPath agentPath) {
        this.agentPath = agentPath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getTs() {
        return ts;
    }

    public void setTs(Integer ts) {
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentToken that = (AgentToken) o;

        return agentPath != null ? agentPath.equals(that.agentPath) : that.agentPath == null;
    }

    @Override
    public int hashCode() {
        return agentPath != null ? agentPath.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "AgentToken{" +
            "agentPath=" + agentPath +
            ", token='" + token + '\'' +
            ", ts=" + ts +
            '}';
    }
}
