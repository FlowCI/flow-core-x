/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.job.controller;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.common.exception.ArgumentException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    protected static final String DefaultPage = "0";

    protected static final String DefaultSize = "20";

    protected static final String ParameterLatest = "latest";

    @Autowired
    protected FlowService flowService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected StepService stepService;

    protected Job getJob(String id) {
        return jobService.get(id);
    }

    protected Job getJob(String name, String buildNumberOrLatest) {
        Flow flow = flowService.get(name);

        if (ParameterLatest.equals(buildNumberOrLatest)) {
            return jobService.getLatest(flow.getId());
        }

        try {
            long buildNumber = Long.parseLong(buildNumberOrLatest);
            return jobService.get(flow.getId(), buildNumber);
        } catch (NumberFormatException e) {
            throw new ArgumentException("Build number must be a integer");
        }
    }
}
