package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.AgentProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentProfileDao extends MongoRepository<AgentProfile, String> {
}
