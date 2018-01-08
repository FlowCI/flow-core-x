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

import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.core.dao.BaseDao;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import java.math.BigInteger;
import java.util.List;

/**
 * @author lhl
 */
public interface NodeResultDao extends BaseDao<NodeResultKey, NodeResult> {

    /**
     * Get node result by job id, status and node tag
     */
    NodeResult get(BigInteger jobId, NodeStatus status, NodeTag tag);

    /**
     * Get node result by job id and step order
     */
    NodeResult get(BigInteger jobId, Integer stepOrder);

    /**
     * List node result for job
     */
    List<NodeResult> list(BigInteger jobId);

    Page<NodeResult> list(BigInteger jobId, Pageable pageable);

    /**
     * Update status to all node result by job id
     */
    int update(BigInteger jobId, NodeStatus target);

    /**
     * delete nodeResult by jobIds
     *
     * @param jobIds jobIds
     */
    void delete(List<BigInteger> jobIds);
}
