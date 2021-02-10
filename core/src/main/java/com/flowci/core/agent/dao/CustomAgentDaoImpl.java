package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.Agent;
import com.flowci.util.ObjectsHelper;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;
import java.util.List;

public class CustomAgentDaoImpl implements CustomAgentDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public long updateAllStatus(Agent.Status status) {
        UpdateResult r = operations.updateMulti(new Query(), new Update().set("status", status), Agent.class);
        return r.getModifiedCount();
    }

    @Override
    public List<Agent> findAll(Collection<String> tags, Collection<Agent.Status> statuses) {
        Query q = new Query();

        if (ObjectsHelper.hasCollection(tags)) {
            q.addCriteria(Criteria.where("tags").in(tags));
        } else {
            q.addCriteria(Criteria.where("tags.0").exists(false));
        }

        if (ObjectsHelper.hasCollection(statuses)) {
            q.addCriteria(Criteria.where("status").in(statuses));
        }

        return operations.find(q, Agent.class);
    }


}
