package com.flowci.core.trigger.dao;

import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggerDeliveryDao extends MongoRepository<TriggerDelivery, String>, CustomizedTriggerDeliveryDao {

}
