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

package com.flow.platform.domain;

import com.google.gson.annotations.Expose;

/**
 * Config for agent
 *
 * @author gy@fir.im
 */
public class AgentSettings extends Jsonable {

    /**
     * Set agent zone and name
     */
    @Expose
    private AgentPath agentPath;

    /**
     * web socket url for upload real time logging
     */
    @Expose
    private String webSocketUrl;

    /**
     * Url for report cmd status
     */
    @Expose
    private String cmdStatusUrl;

    /**
     * Url for upload full zipped cmd log
     */
    @Expose
    private String cmdLogUrl;

    /**
     * Url for zookeeper
     */
    @Expose
    private String zookeeperUrl;

    public AgentSettings() {
    }

    public AgentSettings(String webSocketUrl, String cmdStatusUrl, String cmdLogUrl) {
        this.webSocketUrl = webSocketUrl;
        this.cmdStatusUrl = cmdStatusUrl;
        this.cmdLogUrl = cmdLogUrl;
    }

    public AgentSettings(String webSocketUrl, String cmdStatusUrl, String cmdLogUrl, String zookeeperUrl) {
        this.webSocketUrl = webSocketUrl;
        this.cmdStatusUrl = cmdStatusUrl;
        this.cmdLogUrl = cmdLogUrl;
        this.zookeeperUrl = zookeeperUrl;
    }

    public AgentPath getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(AgentPath agentPath) {
        this.agentPath = agentPath;
    }

    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    public void setZookeeperUrl(String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public String getCmdStatusUrl() {
        return cmdStatusUrl;
    }

    public void setCmdStatusUrl(String cmdStatusUrl) {
        this.cmdStatusUrl = cmdStatusUrl;
    }

    public String getCmdLogUrl() {
        return cmdLogUrl;
    }

    public void setCmdLogUrl(String cmdLogUrl) {
        this.cmdLogUrl = cmdLogUrl;
    }

    @Override
    public String toString() {
        return "AgentSettings{" +
            ", webSocketUrl='" + webSocketUrl + '\'' +
            ", cmdStatusUrl='" + cmdStatusUrl + '\'' +
            ", cmdLogUrl='" + cmdLogUrl + '\'' +
            "} ";
    }
}
