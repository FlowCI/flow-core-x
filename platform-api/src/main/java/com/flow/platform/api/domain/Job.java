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

package com.flow.platform.api.domain;

import com.flow.platform.api.domain.adaptor.EnvAdaptor;
import com.flow.platform.core.dao.adaptor.MapAdaptor;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class Job extends EnvObject {

    private BigInteger id;

    @Expose
    private String nodePath;

    private String sessionId;

    private String cmdId;

    @Expose
    private String nodeName;

    @Expose
    private String branch;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    @Expose
    private Integer number;

    @Expose
    private NodeResult result;

    public Job(BigInteger id) {
        this.id = id;
    }

    public Job() {
    }

    public NodeResult getResult() {
        return result;
    }

    public void setResult(NodeResult result) {
        this.result = result;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Job job = (Job) o;

        return id.equals(job.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Job{" +
            "id='" + id + '\'' +
            ", nodePath='" + nodePath + '\'' +
            ", sessionId='" + sessionId + '\'' +
            ", cmdId='" + cmdId + '\'' +
            '}';
    }
}
