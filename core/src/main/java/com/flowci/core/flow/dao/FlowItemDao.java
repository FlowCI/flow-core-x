package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FlowItemDao extends MongoRepository<FlowItem, String> {

    List<FlowItem> findAllByIdInOrderByCreatedAt(Collection<String> ids);

    boolean existsByName(String name);
}
