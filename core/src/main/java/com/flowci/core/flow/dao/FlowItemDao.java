package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowItemDao extends MongoRepository<FlowItem, String> {
}
