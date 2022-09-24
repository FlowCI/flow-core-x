package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlowGroupDao extends MongoRepository<FlowGroup, String> {

    Optional<FlowGroup> findByName(String name);

    List<FlowGroup> findAllByIdIn(Collection<String> id);

    void deleteByName(String name);
}
