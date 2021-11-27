package com.flowci.core.trigger.dao;

import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomizedTriggerDeliveryDaoImpl implements CustomizedTriggerDeliveryDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void addDelivery(String triggerId, TriggerDelivery.Item item) {
        Query q = new Query();
        q.addCriteria(Criteria.where("triggerId").is(triggerId));

        Update u = new Update();
        u.addToSet("deliveries", item);

        operations.findAndModify(q, u, TriggerDelivery.class);
    }

    @Override
    public Page<TriggerDelivery.Item> listDeliveries(String triggerId, PageRequest pageRequest) {
        TypedAggregation<TriggerDelivery> aggregation = Aggregation.newAggregation(
                TriggerDelivery.class,
                Aggregation.match(Criteria.where("triggerId").is(triggerId)),
                Aggregation.project().and(ArrayOperators.Slice.sliceArrayOf("deliveries").itemCount(5)).as("deliveries")
                        .andInclude("triggerId")
                        .andInclude("id")
        );

        AggregationResults<TriggerDelivery> results = operations.aggregate(aggregation, TriggerDelivery.class);
        TriggerDelivery delivery = results.getUniqueMappedResult();
        return new PageImpl<>(delivery.getDeliveries(), pageRequest, 10);
    }
}
