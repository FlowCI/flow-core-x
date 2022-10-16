/*
 * Copyright 2021 flow.ci
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

package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class CustomJobAgentDaoImpl implements CustomJobAgentDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void addFlowToAgent(String jobId, String agentId, String flowPath) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().addToSet(key, flowPath),
                JobAgent.class);
    }

    @Override
    public void removeFlowFromAgent(String jobId, String agentId, String flowPath) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().pull(key, flowPath),
                JobAgent.class);
    }

    @Override
    public void removeAgent(String jobId, String agentId) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().unset(key),
                JobAgent.class);
    }
}
