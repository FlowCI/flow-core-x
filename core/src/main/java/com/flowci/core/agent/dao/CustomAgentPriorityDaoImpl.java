package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.AgentPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomAgentPriorityDaoImpl implements CustomAgentPriorityDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void addJob(String selectorId, String flowId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where("selectorId").is(selectorId));

        Update u = new Update();
        u.addToSet("queue." + flowId, buildNumber);

        operations.findAndModify(q, u, AgentPriority.class);
    }

    @Override
    public void removeJob(String selectorId, String flowId, Long buildNumber) {
        Query q = new Query();
        q.addCriteria(Criteria.where("selectorId").is(selectorId));

        Update u = new Update();
        u.pull("queue." + flowId, buildNumber);

        operations.findAndModify(q, u, AgentPriority.class);
    }

    @Override
    public long findMinBuildNumber(String selectorId, String flowId) {
        String filed = "queue." + flowId;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("selectorId").is(selectorId)),
                Aggregation.project().and(AccumulatorOperators.Min.minOf(filed)).as("number").andExclude("_id")
        );

        AggregationResults<NumberResult> results = operations.aggregate(aggregation, "agent_priority", NumberResult.class);
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
