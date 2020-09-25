package com.flowci.core.agent.dao;

import com.flowci.domain.Agent;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomAgentDaoImpl implements CustomAgentDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public long updateAllStatus(Agent.Status status) {
        UpdateResult r = operations.updateMulti(new Query(), new Update().set("status", status), Agent.class);
        return r.getModifiedCount();
    }
}
