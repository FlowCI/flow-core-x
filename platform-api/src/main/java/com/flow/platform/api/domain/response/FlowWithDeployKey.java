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

import com.flow.platform.api.domain.EnvObject;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;

/**
 * @author yh@firim
 */
public class FlowWithDeployKey extends EnvObject {

    // flow name
    @Expose
    private String name;
    @Expose
    private String username;
    @Expose
    private ZonedDateTime createdAt;
    @Expose
    private String deployKeyName;
    @Expose
    private String deployKey;

    public FlowWithDeployKey(String name, String username, ZonedDateTime createdTime) {
        this.name = name;
        this.username = username;
        this.createdAt = createdTime;
    }

    public FlowWithDeployKey(String name, String username, ZonedDateTime createdTime,
        String deployKeyName, String deployKey) {
        this.name = name;
        this.username = username;
        this.createdAt = createdTime;
        this.deployKeyName = deployKeyName;
        this.deployKey = deployKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDeployKeyName() {
        return deployKeyName;
    }

    public void setDeployKeyName(String deployKeyName) {
        this.deployKeyName = deployKeyName;
    }

    public String getDeployKey() {
        return deployKey;
    }

    public void setDeployKey(String deployKey) {
        this.deployKey = deployKey;
    }

}
