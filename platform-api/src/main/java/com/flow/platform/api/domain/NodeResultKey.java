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

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author lhl
 */
public class NodeResultKey implements Serializable {

    @Expose
    private BigInteger jobId;

    @Expose
    private String path;

    public BigInteger getJobId() {
        return jobId;
    }

    public void setJobId(BigInteger jobId) {
        this.jobId = jobId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public NodeResultKey() {
    }

    public NodeResultKey(BigInteger jobId, String path) {
        this.jobId = jobId;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeResultKey that = (NodeResultKey) o;

        if (!jobId.equals(that.jobId)) {
            return false;
        }
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = jobId.hashCode();
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NodeResultKey{" +
            "jobId=" + jobId +
            ", path='" + path + '\'' +
            '}';
    }
}
