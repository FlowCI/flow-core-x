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

package com.flow.platform.api.domain.response;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;

/**
 * @author yh@firim
 */
public class FlowWithDeployKey extends Jsonable {

    @Expose
    private String flowName;
    @Expose
    private String creator;
    @Expose
    private ZonedDateTime createdAt;
    @Expose
    private String webhookUrl;
    @Expose
    private String deployKey;

    public FlowWithDeployKey(String flowName, String creator, ZonedDateTime createdAt, String webhookUrl,
        String deployKey) {
        this.flowName = flowName;
        this.creator = creator;
        this.createdAt = createdAt;
        this.webhookUrl = webhookUrl;
        this.deployKey = deployKey;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getDeployKey() {
        return deployKey;
    }

    public void setDeployKey(String deployKey) {
        this.deployKey = deployKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowWithDeployKey that = (FlowWithDeployKey) o;

        return flowName != null ? flowName.equals(that.flowName) : that.flowName == null;
    }

    @Override
    public int hashCode() {
        return flowName != null ? flowName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FlowWithDeployKey{" +
            "flowName='" + flowName + '\'' +
            ", creator='" + creator + '\'' +
            ", createdAt=" + createdAt +
            ", webhookUrl='" + webhookUrl + '\'' +
            '}';
    }
}
