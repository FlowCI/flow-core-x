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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Config for agent
 *
 * @author gy@fir.im
 */
@NoArgsConstructor
@ToString(of = {"agentPath", "webSocketUrl", "cmdStatusUrl", "cmdLogUrl", "zookeeperUrl"})
public class AgentSettings extends Jsonable {

    /**
     * Set agent zone and name
     */
    @Getter
    @Setter
    @Expose
    private AgentPath agentPath;

    /**
     * web socket url for upload real time logging
     */
    @Getter
    @Setter
    @Expose
    private String webSocketUrl;

    /**
     * Url for report cmd status
     */
    @Getter
    @Setter
    @Expose
    private String cmdStatusUrl;

    /**
     * Url for upload full zipped cmd log
     */
    @Getter
    @Setter
    @Expose
    private String cmdLogUrl;

    /**
     * Url for zookeeper
     */
    @Getter
    @Setter
    @Expose
    private String zookeeperUrl;

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
}
