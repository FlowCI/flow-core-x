/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.util;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.ExecutedCmd.Status;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * @author yang
 */
public abstract class StatusHelper {

    private final static Map<Status, Job.Status> StatusMapping = ImmutableMap.<Status, Job.Status>builder()
        .put(Status.PENDING, Job.Status.PENDING)
        .put(Status.RUNNING, Job.Status.RUNNING)
        .put(Status.SUCCESS, Job.Status.SUCCESS)
        .put(Status.SKIPPED, Job.Status.SUCCESS)
        .put(Status.EXCEPTION, Job.Status.FAILURE)
        .put(Status.KILLED, Job.Status.CANCELLED)
        .put(Status.TIMEOUT, Job.Status.TIMEOUT)
        .build();

    public static Job.Status convert(ExecutedCmd executedCmd) {
        // to handle allow failure
        if (executedCmd.isSuccess()) {
            return Job.Status.SUCCESS;
        }

        return StatusMapping.get(executedCmd.getStatus());
    }
}
