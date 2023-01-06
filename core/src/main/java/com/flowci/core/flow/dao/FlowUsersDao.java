package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowUsers;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowUsersDao extends MongoRepository<FlowUsers, String>, CustomizedFlowUserDao {
}
