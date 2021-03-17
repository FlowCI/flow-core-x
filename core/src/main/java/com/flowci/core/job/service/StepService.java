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
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;

import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
public interface StepService {

    /**
     * Create steps with init value
     */
    void init(Job job);

    /**
     * Get executed cmd for job and node
     */
    Step get(String jobId, String nodePath);

    /**
     * Get executed cmd from step id
     */
    Step get(String id);

    /**
     * List step of executed cmd for job
     */
    List<Step> list(Job job);

    /**
     * List step by job and status
     */
    List<Step> list(Job job, Collection<Executed.Status> status);

    /**
     * Get step list in string, {name}={stats};{name}={stats}
     * No steps after current node
     */
    String toVarString(Job job, Step current);

    /**
     * Batch set status for step and return step list
     */
    Collection<Step> toStatus(Collection<Step> steps, Executed.Status status, String err);

    /**
     * Change step status, and put steps string to job context
     * @param allChildren indicate to update status to all children step
     */
    Step toStatus(Step entity, Step.Status status, String err, boolean allChildren);

    /**
     * To update properties are related with cmd executed result
     */
    void resultUpdate(Step stepFromAgent);

    /**
     * Delete steps by flow id
     */
    Long delete(Flow flow);

    /**
     * Delete steps by job
     */
    Long delete(Job job);
}
