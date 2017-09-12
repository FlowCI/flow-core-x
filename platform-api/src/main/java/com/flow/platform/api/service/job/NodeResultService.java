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
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.domain.Cmd;
import java.math.BigInteger;
import java.util.List;

/**
 * @author yh@firim
 */
public interface NodeResultService {

    /**
     * Create all empty nodes results by job
     *
     * @return List of node result for job
     */
    List<NodeResult> create(Job job);

    /**
     * find node by node path
     */
    NodeResult find(String path, BigInteger jobId);

    /**
     * List all node results for job
     */
    List<NodeResult> list(Job job);

    /**
     * Save node result to db
     */
    void save(NodeResult result);

    /**
     * Update node result and recursive bottom up update parent node result by cmd
     */
    NodeResult update(Job job, Node node, Cmd cmd);
}