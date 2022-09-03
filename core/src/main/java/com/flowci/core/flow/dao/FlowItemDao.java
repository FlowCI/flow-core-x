package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface FlowItemDao extends MongoRepository<FlowItem, String> {

    Set<FlowItem> findAllByIdIn(Collection<String> ids);

    boolean existsByName(String name);
}
