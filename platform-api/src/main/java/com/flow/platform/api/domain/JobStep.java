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

import java.math.BigInteger;

public class JobStep extends NodeResult {

    private Boolean allowFailure = false;

    private String plugin;

    public Boolean getAllowFailure() {
        return allowFailure;
    }

    public void setAllowFailure(Boolean allowFailure) {
        this.allowFailure = allowFailure;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public JobStep(BigInteger jobId, String path) {

        super(jobId, path);
    }

    public JobStep() {
    }

    @Override
    public String toString() {
        return "JobStep{" +
            "plugin='" + plugin + '\'' +
            ", outputs=" + outputs +
            ", duration=" + duration +
            ", exitCode=" + exitCode +
            ", logPaths=" + logPaths +
            ", status=" + status +
            ", cmdId='" + cmdId + '\'' +
            ", startTime=" + startTime +
            '}';
    }


}
