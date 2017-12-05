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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.api.domain.node.NodeTree;
import java.math.BigInteger;
import java.util.List;

/**
 * @author lhl
 */
public interface JobNodeService {

    /**
     * save yml to db
     */
    void save(Job job, String yml);


    /**
     * get node tree by job
     */
    NodeTree get(Job job);

    /**
     * Get job yml data
     */
    JobYml find(Job job);

    /**
     * Delete job node data by list of job id
     */
    void delete(List<BigInteger> jobIds);

}
