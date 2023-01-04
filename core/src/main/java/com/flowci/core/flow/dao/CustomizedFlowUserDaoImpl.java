package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;

public class CustomizedFlowUserDaoImpl implements CustomizedFlowUserDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<String> findAllFlowsByUserEmail(String email) {
        Query q = Query.query(Criteria.where("users").in(email));
        q.fields().exclude("users");

        List<FlowUsers> lists = mongoTemplate.find(q, FlowUsers.class);
        List<String> ids = new LinkedList<>();

        for (FlowUsers item : lists) {
            ids.add(item.getId());
        }

        return ids;
    }

    @Override
    public void insert(String flowId, Collection<String> emails) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        Update u = new Update().addToSet("users").each(emails);
        mongoTemplate.upsert(q, u, FlowUsers.class);
    }

    @Override
    public void remove(String flowId, Collection<String> emails) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        Update u = new Update();
        u.pullAll("users", emails.toArray());
        mongoTemplate.updateFirst(q, u, FlowUsers.class);
    }

    @Override
    public boolean exist(String flowId, String email) {
        Query q = new Query();
        q.addCriteria(Criteria.where("_id").is(flowId));
        q.addCriteria(Criteria.where("users").is(email));
        return mongoTemplate.exists(q, FlowUsers.class);
    }
}
