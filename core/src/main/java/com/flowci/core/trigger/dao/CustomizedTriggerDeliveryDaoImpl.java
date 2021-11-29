package com.flowci.core.trigger.dao;

import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

public class CustomizedTriggerDeliveryDaoImpl implements CustomizedTriggerDeliveryDao {

    private final static String FieldDeliveries = "deliveries";

    private final static String FieldDeliveriesTotal = "deliveriesTotal";

    private final static String FieldTriggerID = "triggerId";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void addDelivery(String triggerId, TriggerDelivery.Item item, int max) {
        mongoTemplate.findAndModify(
                new Query()
                        .addCriteria(Criteria.where(FieldTriggerID).is(triggerId)),
                new Update()
                        .push(FieldDeliveries).atPosition(0).each(item),
                TriggerDelivery.class);

        mongoTemplate.findAndModify(
                new Query()
                        .addCriteria(Criteria.where(FieldTriggerID).is(triggerId))
                        .addCriteria(Criteria.where(FieldDeliveries + "." + max).exists(true)),
                new Update()
                        .pop(FieldDeliveries, Update.Position.LAST),
                TriggerDelivery.class
        );
    }

    @Override
    public Page<TriggerDelivery.Item> listDeliveries(String triggerId, PageRequest pageRequest) {
        TypedAggregation<TriggerDelivery> aggregation = Aggregation.newAggregation(
                TriggerDelivery.class,
                Aggregation.match(Criteria.where(FieldTriggerID).is(triggerId)),
                Aggregation.project()
                        .and(ArrayOperators.Slice.sliceArrayOf(FieldDeliveries)
                                .offset((int) pageRequest.getOffset())
                                .itemCount(pageRequest.getPageSize()))
                        .as(FieldDeliveries)
                        .and(ArrayOperators.Size.lengthOfArray(FieldDeliveries)).as(FieldDeliveriesTotal)
        );

        AggregationResults<TriggerDelivery> results = mongoTemplate.aggregate(aggregation, TriggerDelivery.class);
        TriggerDelivery delivery = results.getUniqueMappedResult();

        List<TriggerDelivery.Item> deliveries = delivery.getDeliveries();
        return new PageImpl<>(deliveries, pageRequest, delivery.getDeliveriesTotal());
    }
}
