package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.AgentPriority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentPriorityDao extends MongoRepository<AgentPriority, String>, CustomAgentPriorityDao {

    Optional<AgentPriority> findBySelectorId(String selectorId);
}
