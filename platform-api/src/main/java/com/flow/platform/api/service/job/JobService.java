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
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.util.git.model.GitEventType;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author yh@firim
 */
public interface JobService {

    /**
     * find by node path and number
     *
     * @return job with children node result
     */
    Job find(String path, Integer number);

    /**
     * Find by job id
     *
     * @return job with children node result
     */
    Job find(BigInteger jobId);

    /**
     * Get job yml content
     */
    String findYml(String path, Integer number);

    /**
     * delete jobs by flowPath
     */
    void deleteJob(String path);

    /**
     * List all jobs by given path
     *
     * @param paths job node path
     * @param latestOnly is only load latest job
     */
    List<Job> list(List<String> paths, boolean latestOnly);

    /**
     * list node results
     */
    List<NodeResult> listNodeResult(String path, Integer number);

    /**
     * create job from node path, copy yml to job yml
     * request agent session from control center
     *
     * @param path any node path
     * @param envs the input environment variables
     * @return job with children node result
     */
    Job createJob(String path, GitEventType eventType, Map<String, String> envs);

    /**
     * handle callback
     **/
    void callback(CmdCallbackQueueItem cmdQueueItem);
    void enterQueue(CmdCallbackQueueItem cmdQueueItem);

    /**
     * stop job
     */
    Job stopJob(String name, Integer buildNumber);

    /**
     * update job
     */
    Job update(Job job);

    /**
     * check timeout job
     */
    void checkTimeoutTask();
}
