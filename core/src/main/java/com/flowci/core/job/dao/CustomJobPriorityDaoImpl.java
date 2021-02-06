package com.flowci.core.job.dao;

import com.flowci.core.job.domain.Job;
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
    public List<Job> findAllMinBuildNumber() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project().and(AccumulatorOperators.Min.minOf(FieldQueue)).as("buildNumber").andExclude("_id")
        );

        return operations.aggregate(aggregation, "job_priority", Job.class).getMappedResults();
    }

    private static class NumberResult {

        public Long number;
    }
}
