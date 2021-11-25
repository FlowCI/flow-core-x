package com.flowci.core.trigger.dao;

import com.flowci.core.trigger.domain.Trigger;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriggerDao extends MongoRepository<Trigger, String> {

    Optional<Trigger> findByName(String name);

    List<Trigger> findAllByEvent(Trigger.Event action);

    void deleteByName(String name);
}
