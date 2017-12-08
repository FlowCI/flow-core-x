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

package com.flow.platform.api.dao.job;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.core.dao.BaseDao;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * @author yh@firim
 */
public interface JobDao extends BaseDao<BigInteger, Job> {

    /**
     * List job by session id and node status
     *
     * @return Job with NodeResult
     */
    List<Job> list(List<String> sessionId, NodeStatus nodeStatus);

    /**
     * List job by node paths
     *
     * @param paths node path or null
     * @return Job with Node Result
     */
    List<Job> listByPath(List<String> paths);

    /**
     * List jobs by job status
     *
     * @param status status set
     * @return Job
     */
    List<Job> listByStatus(EnumSet<JobStatus> status);

    /**
     * delete jobs by node path
     *
     * @param path node path
     */
    int deleteJob(String path);

    /**
     * get jobIds by node path
     *
     * @param path node path
     */
    List<BigInteger> findJobIdsByPath(String path);

    /**
     * get latest job by flow path
     *
     * @param paths node path or null for all latest jobs
     */
    List<Job> latestByPath(List<String> paths);

    /**
     * get job from node path and number
     */
    Job get(String path, Integer number);

    /**
     * get job by session id
     */
    Job get(String sessionId);

    /**
     * get max build number for node path
     */
    Integer maxBuildNumber(String path);
}
