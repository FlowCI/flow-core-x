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

package com.flowci.core.job.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobDesc;
import com.flowci.core.job.domain.JobItem;
import com.flowci.core.job.domain.JobYml;
import com.flowci.domain.StringVars;
import org.springframework.data.domain.Page;

import javax.annotation.Nullable;

/**
 * @author yang
 */
public interface JobService {

    /**
     * Init job data and queue for flow
     */
    void init(Flow flow);

    /**
     * Get job by id
     */
    Job get(String id);

    /**
     * Get job desc (flow name, build number)
     *
     * @return desc instance or null
     */
    JobDesc getDesc(String id);

    /**
     * Get job by flow and build number
     */
    Job get(String flowId, Long buildNumber);

    /**
     * Get job yml by job
     */
    JobYml getYml(Job job);

    /**
     * Get latest job
     */
    Job getLatest(String flowId);

    /**
     * List job with fields only shown on the list
     */
    Page<JobItem> list(Flow flow, int page, int size);

    /**
     * Create a job by flow and yml, job status will be PENDING -> LOADING -> CREATED,
     *
     * @throws com.flowci.exception.NotAvailableException with job object in extra field
     */
    Job create(Flow flow, @Nullable String yml, Trigger trigger, StringVars input);

    /**
     * Run job, dispatch to queue, status will be CREATED -> RUNNING
     * @param job
     */
    void start(Job job);

    /**
     * Cancel job, status will be CANCELLING -> CANCELLED,
     * @param job
     */
    void cancel(Job job);

    /**
     * Restart job
     */
    Job rerun(Flow flow, Job job);

    /**
     * Restart job from failure step
     */
    Job rerunFromFailureStep(Flow flow, Job job);

    /**
     * Delete all jobs of the flow within an executor
     */
    void delete(Flow flow);
}

