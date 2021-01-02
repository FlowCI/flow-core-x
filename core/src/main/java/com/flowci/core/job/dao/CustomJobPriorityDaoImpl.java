package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomJobPriorityDaoImpl implements CustomJobPriorityDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void addJob(String flowId, String selectorId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where("flowId").is(flowId));

        Update u = new Update();
        u.addToSet("queue." + selectorId, buildNumber);

        operations.findAndModify(q, u, JobPriority.class);
    }

    @Override
    public void removeJob(String flowId, String selectorId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where("flowId").is(flowId));

        Update u = new Update();
        u.pull("queue." + selectorId, buildNumber);

        operations.findAndModify(q, u, JobPriority.class);
    }

    @Override
    public long findMinBuildNumber(String flowId, String selectorId) {
        String filed = "queue." + selectorId;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("flowId").is(flowId)),
                Aggregation.project().and(AccumulatorOperators.Min.minOf(filed)).as("number").andExclude("_id")
        );

        AggregationResults<NumberResult> results = operations.aggregate(aggregation, "job_priority", NumberResult.class);
        NumberResult numberResult = results.getUniqueMappedResult();
        if (numberResult == null) {
            return 0;
        }

        return numberResult.number;
    }

    private static class NumberResult {

        public Long number;
    }
}
