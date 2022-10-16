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

import com.flowci.core.job.domain.JobKey;
import com.flowci.core.job.domain.JobPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

public class CustomJobPriorityDaoImpl implements CustomJobPriorityDao {

    private final static String FieldFlowId = "flowId";

    private final static String FieldQueue = "queue";

    @Autowired
    private MongoOperations operations;

    @Override
    public void addJob(String flowId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where(FieldFlowId).is(flowId));

        Update u = new Update();
        u.addToSet(FieldQueue, buildNumber);

        operations.findAndModify(q, u, JobPriority.class);
    }

    @Override
    public void removeJob(String flowId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where(FieldFlowId).is(flowId));

        Update u = new Update();
        u.pull(FieldQueue, buildNumber);

        operations.findAndModify(q, u, JobPriority.class);
    }

    /**
     * Return min build number
     * Return MAX value of long if no min number found
     */
    @Override
    public long findMinBuildNumber(String flowId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(FieldFlowId).is(flowId)),
                Aggregation.project().and(AccumulatorOperators.Min.minOf(FieldQueue)).as("number").andExclude("_id")
        );

        AggregationResults<NumberResult> results = operations.aggregate(aggregation, "job_priority", NumberResult.class);
        NumberResult numberResult = results.getUniqueMappedResult();
        if (numberResult == null) {
            return Long.MAX_VALUE;
        }

        return numberResult.number == null ? Long.MAX_VALUE : numberResult.number;
    }

    @Override
    public List<JobKey> findAllMinBuildNumber() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project().and(AccumulatorOperators.Min.minOf(FieldQueue))
                        .as("buildNumber")
                        .andInclude("flowId")
                        .andExclude("_id")
        );

        return operations.aggregate(aggregation, "job_priority", JobKey.class).getMappedResults();
    }

    private static class NumberResult {

        public Long number;
    }
}
