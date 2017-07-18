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
public class AgentConfig extends Jsonable {

    /**
     * Url for socket io realtime logging
     */
    private String loggingUrl;

    /**
     * Url for report cmd status
     */
    private String cmdStatusUrl;

    /**
     * Url for upload full zipped cmd log
     */
    private String cmdLogUrl;

    public AgentConfig() {
    }

    public AgentConfig(String loggingUrl, String cmdStatusUrl, String cmdLogUrl) {
        this.loggingUrl = loggingUrl;
        this.cmdStatusUrl = cmdStatusUrl;
        this.cmdLogUrl = cmdLogUrl;
    }

    public String getLoggingUrl() {
        return loggingUrl;
    }

    public void setLoggingUrl(String loggingUrl) {
        this.loggingUrl = loggingUrl;
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
        return "AgentConfig{" +
                "loggingUrl='" + loggingUrl + '\'' +
                ", cmdStatusUrl='" + cmdStatusUrl + '\'' +
                ", cmdLogUrl='" + cmdLogUrl + '\'' +
                "} " + super.toString();
    }
}
