package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowGroupDao extends MongoRepository<FlowGroup, String> {
}
