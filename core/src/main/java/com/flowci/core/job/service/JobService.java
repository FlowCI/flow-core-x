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
import com.flowci.core.job.domain.JobItem;
import com.flowci.core.job.domain.JobYml;
import com.flowci.domain.StringVars;
import org.springframework.data.domain.Page;

/**
 * @author yang
 */
public interface JobService {

    /**
     * Get job by id
     */
    Job get(String id);

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
     * Create a job by flow and yml
     *
     * @throws com.flowci.exception.NotAvailableException with job object in extra field
     */
    Job create(Flow flow, String yml, Trigger trigger, StringVars input);

    /**
     * Send to job queue
     */
    Job start(Job job);

    /**
     * Force to stop the job if it's running
     */
    Job cancel(Job job);

    /**
     * Delete all jobs of the flow within an executor
     */
    void delete(Flow flow);

    /**
     * Job is expired compare to now
     */
    boolean isExpired(Job job);

    /**
     * Save job, with new status, and publish job status change event
     */
    Job setJobStatusAndSave(Job job, Job.Status newStatus, String message);
}

