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

package com.flow.platform.api.util;

/**
 * @author yh@firim
 */

public class PlatformURL {

    private String cmdUrl;

    private String queueUrl;

    private String cmdStopUrl;

    private String agentUrl;

    private String agentInfoUrl;

    private String sysInfoUrl;

    private String sysIndexUrl;

    private String cmdDownloadLogUrl;

    public PlatformURL(String baseURL) {
        cmdUrl = String.format("%s%s", baseURL, "cmd/send");
        queueUrl = String.format("%s%s", baseURL, "cmd/queue/send");
        cmdStopUrl = String.format("%s%s", baseURL, "cmd/stop/");
        cmdDownloadLogUrl = String.format("%s%s", baseURL, "cmd/log/download");
        agentUrl = String.format("%s%s", baseURL, "agent/list");
        agentInfoUrl = String.format("%s%s", baseURL, "agent/findAgentBySessionId");
        sysIndexUrl = String.format("%s%s", baseURL, "index");
        sysInfoUrl = String.format("%s%s", baseURL, "sys/info");
    }

    public String getCmdUrl() {
        return cmdUrl;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getCmdStopUrl() {
        return cmdStopUrl;
    }

    public String getCmdDownloadLogUrl() {
        return cmdDownloadLogUrl;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public String getAgentInfoUrl() {
        return agentInfoUrl;
    }

    public String getSysIndexUrl() {
        return sysIndexUrl;
    }

    public String getSysInfoUrl() {
        return sysInfoUrl;
    }
}
