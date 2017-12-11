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
package com.flow.platform.api.service.job;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.GitEnvs;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author yh@firim
 */
public interface JobService {

    /**
     * Required env variable envs for create job
     */
    Set<EnvKey> REQUIRED_ENVS = ImmutableSet.of(
        FlowEnvs.FLOW_STATUS,
        FlowEnvs.FLOW_YML_STATUS,
        GitEnvs.FLOW_GIT_URL,
        GitEnvs.FLOW_GIT_SOURCE
    );

    /**
     * find by node path and number
     *
     * @return job with children node result
     */
    Job find(String path, Long number);

    /**
     * Find by job id
     *
     * @return job with children node result
     */
    Job find(BigInteger jobId);

    /**
     * Find by agent session id
     */
    Job find(String sessionId);

    /**
     * Get job yml content
     */
    String findYml(String path, Long number);

    /**
     * delete jobs by flowPath
     */
    void delete(String path);

    /**
     * List all jobs by given path
     *
     * @param paths job node path
     * @param latestOnly is only load latest job
     */
    List<Job> list(List<String> paths, boolean latestOnly);

    /**
     * Create job by yml which from flow
     */
    Job createFromFlowYml(String path, JobCategory eventType, Map<String, String> envs, User creator);

    /**
     * Create job after loading yml from git repo, in async mode
     *
     * @param path any node path
     * @param eventType the trigger type
     * @param envs the input environment variables
     * @param creator the user who create job
     * @param onJobCreated callback
     */
    void createWithYmlLoad(String path,
                           JobCategory eventType,
                           Map<String, String> envs,
                           User creator,
                           Consumer<Job> onJobCreated);

    /**
     * Process cmd callback from queue
     **/
    void callback(CmdCallbackQueueItem cmdQueueItem);

    /**
     * Send cmd callback item to queue
     */
    void enqueue(CmdCallbackQueueItem cmdQueueItem, int priority);

    /**
     * stop job
     */
    Job stop(String name, Long buildNumber);

    /**
     * update job
     */
    Job update(Job job);

    /**
     * Check job is timeout, and close session if it is timeout
     */
    void checkTimeOut(Job job);

    /**
     * check timeout job
     */
    void checkTimeOutTask();

    /**
     * Set job status and save job instance
     */
    void updateJobStatusAndSave(Job job, JobStatus newStatus);
}
