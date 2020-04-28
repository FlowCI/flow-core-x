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
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.tree.StepNode;

import java.util.List;

/**
 * @author yang
 */
public interface StepService {

    List<ExecutedCmd> init(Job job);

    /**
     * Get executed cmd for job and node
     */
    ExecutedCmd get(String jobId, String nodePath);

    /**
     * Get executed cmd from cmd id
     */
    ExecutedCmd get(String id);

    /**
     * List step of executed cmd for job
     */
    List<ExecutedCmd> list(Job job);

    /**
     * Get step list in string, {name}={stats};{name}={stats}
     * No steps after current node
     */
    String toVarString(Job job, StepNode current);

    /**
     * Change step status, and put steps string to job context
     */
    ExecutedCmd statusChange(String jobId, String nodePath, ExecutedCmd.Status status, String err);

    ExecutedCmd statusChange(ExecutedCmd entity, ExecutedCmd.Status status, String err);

    /**
     * To update properties are related with cmd executed result
     */
    void resultUpdate(ExecutedCmd result);

    /**
     * Delete steps by flow id
     */
    Long delete(Flow flow);

    /**
     * Delete steps by job
     */
    Long delete(Job job);
}
