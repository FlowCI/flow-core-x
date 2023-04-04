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

package com.flowci.core.job.manager;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobYml;
import com.flowci.parser.v1.FlowNode;
import com.flowci.parser.v1.NodeTree;

/**
 * @author yang
 */
public interface YmlManager {

    FlowNode parse(JobYml jobYml);

    FlowNode parse(String ...raw);


    JobYml get(Job job);

    /**
     * Create job yaml from flow yaml
     */
    JobYml create(JobYml jobYml);

    /**
     * Get node tree from job
     */
    NodeTree getTree(Job job);

    /**
     * Get node tree from job id
     */
    NodeTree getTree(String jobId);

    /**
     * Delete yaml for job
     */
    void delete(Job job);
}
