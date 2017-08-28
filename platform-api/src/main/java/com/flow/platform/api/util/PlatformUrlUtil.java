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

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */

@Component(value = "platformUrlUtil")
public class PlatformUrlUtil {

    @Value(value = "${platform.url}")
    private String baseUrl;

    private String cmdUrl;
    private String queueUrl;
    private String cmdStopUrl;
    private String agentUrl;
    private String agentShutdownUrl;
    private String sysinfoUrl;

    @PostConstruct
    private void init() {
        cmdUrl = String.format("%s%s", baseUrl, "cmd/send");
        queueUrl = String.format("%s%s", baseUrl, "cmd/queue/send");
        cmdStopUrl = String.format("%s%s", baseUrl, "cmd/stop/");
        agentUrl = String.format("%s%s", baseUrl, "agent/list");
        agentShutdownUrl = String.format("%s%s", baseUrl, "agent/shutdown");
        sysinfoUrl = String.format("%s%s", baseUrl, "sys/info");
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

    public String getAgentUrl() {
        return agentUrl;
    }

    public String getAgentShutdownUrl() {
        return agentShutdownUrl;
    }

    public String getSysinfoUrl() {
        return sysinfoUrl;
    }
}
