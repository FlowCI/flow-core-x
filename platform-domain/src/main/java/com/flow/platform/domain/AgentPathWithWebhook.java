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
 * @author yang
 */
public class AgentPathWithWebhook extends AgentPath {

    private String webhook;

    public AgentPathWithWebhook(AgentPath agentPath, String webhook) {
        this(agentPath.getZone(), agentPath.getName(), webhook);
    }

    public AgentPathWithWebhook(String zone, String name, String webhook) {
        super(zone, name);
        this.webhook = webhook;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }
}
