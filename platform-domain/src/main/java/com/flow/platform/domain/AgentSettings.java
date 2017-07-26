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
     * Host name of rabbitmq for real time logging
     */
    private String loggingQueueHost;

    /**
     * Queue name of rabbitmq for real time logging
     */
    private String loggingQueueName;

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

    public AgentSettings(String loggingQueueHost, String loggingQueueName, String cmdStatusUrl, String cmdLogUrl) {
        this.loggingQueueHost = loggingQueueHost;
        this.loggingQueueName = loggingQueueName;
        this.cmdStatusUrl = cmdStatusUrl;
        this.cmdLogUrl = cmdLogUrl;
    }

    public String getLoggingQueueHost() {
        return loggingQueueHost;
    }

    public void setLoggingQueueHost(String loggingQueueHost) {
        this.loggingQueueHost = loggingQueueHost;
    }

    public String getLoggingQueueName() {
        return loggingQueueName;
    }

    public void setLoggingQueueName(String loggingQueueName) {
        this.loggingQueueName = loggingQueueName;
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
            "loggingQueueHost='" + loggingQueueHost + '\'' +
            ", loggingQueueName='" + loggingQueueName + '\'' +
            ", cmdStatusUrl='" + cmdStatusUrl + '\'' +
            ", cmdLogUrl='" + cmdLogUrl + '\'' +
            "} ";
    }
}
