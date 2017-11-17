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

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Jsonable;
import java.math.BigInteger;

/**
 * @author yh@firim
 */
public class CmdCallbackQueueItem extends Jsonable {

    private final BigInteger jobId;

    private final String path; // node path

    private final Cmd cmd;

    private Integer retryTimes = 0;

    // priority default 1
    private Integer priority = 1;

    public CmdCallbackQueueItem(BigInteger jobId, Cmd cmd) {
        this.jobId = jobId;
        this.cmd = cmd;
        this.path = cmd.getExtra();
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigInteger getJobId() {
        return jobId;
    }

    public String getPath() {
        return path;
    }

    public Cmd getCmd() {
        return cmd;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    /**
     * self plus ++
     */
    public void plus() {
        retryTimes += 1;
    }

    @Override
    public String toString() {
        return "CmdQueueItem{" +
            "jobId='" + jobId + '\'' +
            ", cmd=" + cmd +
            '}';
    }
}
