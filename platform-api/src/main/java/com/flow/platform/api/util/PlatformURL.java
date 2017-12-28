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

import com.flow.platform.util.http.HttpURL;

/**
 * @author yh@firim
 */

public class PlatformURL {

    private final String cmdUrl;

    private final String queueUrl;

    private final String agentUrl;

    private final String agentSettingsUrl;

    private final String sysInfoUrl;

    private final String sysIndexUrl;

    private final String agentCreateUrl;

    private final String cmdDownloadLogUrl;

    private final String agentDeleteUrl;

    private final String agentFindUrl;

    public PlatformURL(String baseURL) {
        queueUrl = HttpURL.build(baseURL).append("cmd/queue/send").toString();
        cmdUrl = HttpURL.build(baseURL).append("cmd/send").toString();
        cmdDownloadLogUrl = HttpURL.build(baseURL).append("cmd/log/download").toString();

        sysIndexUrl = HttpURL.build(baseURL).append("index").toString();
        sysInfoUrl = HttpURL.build(baseURL).append("sys/info").toString();

        agentUrl = HttpURL.build(baseURL).append("agents/list").toString();
        agentCreateUrl = HttpURL.build(baseURL).append("agents/create").toString();
        agentSettingsUrl = HttpURL.build(baseURL).append("agents/settings").toString();
        agentDeleteUrl = HttpURL.build(baseURL).append("agents/delete").toString();
        agentFindUrl = HttpURL.build(baseURL).append("agents/find").toString();
    }

    public String getAgentCreateUrl() {
        return agentCreateUrl;
    }

    public String getAgentSettingsUrl() {
        return agentSettingsUrl;
    }

    public String getCmdUrl() {
        return cmdUrl;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getCmdDownloadLogUrl() {
        return cmdDownloadLogUrl;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public String getSysIndexUrl() {
        return sysIndexUrl;
    }

    public String getSysInfoUrl() {
        return sysInfoUrl;
    }

    public String getAgentDeleteUrl() {
        return agentDeleteUrl;
    }

    public String getAgentFindUrl() {
        return agentFindUrl;
    }

    @Override
    public String toString() {
        return "PlatformURL{" +
            "cmdUrl='" + cmdUrl + '\'' +
            ", queueUrl='" + queueUrl + '\'' +
            ", agentUrl='" + agentUrl + '\'' +
            ", agentSettingsUrl='" + agentSettingsUrl + '\'' +
            ", agentDeleteUrl='" + agentDeleteUrl + '\'' +
            ", agentFindUrl='" + agentFindUrl + '\'' +
            ", sysInfoUrl='" + sysInfoUrl + '\'' +
            ", sysIndexUrl='" + sysIndexUrl + '\'' +
            ", agentCreateUrl='" + agentCreateUrl + '\'' +
            ", cmdDownloadLogUrl='" + cmdDownloadLogUrl + '\'' +
            '}';
    }
}