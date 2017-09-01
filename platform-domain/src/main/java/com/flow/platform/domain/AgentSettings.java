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

/**
 * Config for agent
 *
 * @author gy@fir.im
 */
public class AgentSettings extends Jsonable {

    /**
     * web socket url for upload real time logging
     */
    private String webSocketUrl;

    /**
     * Url for report cmd status
     */
    private String cmdStatusUrl;

    /**
     * Url for upload full zipped cmd log
     */
    private String cmdLogUrl;

    public AgentSettings() {
    }

    public AgentSettings(String webSocketUrl, String cmdStatusUrl, String cmdLogUrl) {
        this.webSocketUrl = webSocketUrl;
        this.cmdStatusUrl = cmdStatusUrl;
        this.cmdLogUrl = cmdLogUrl;
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
