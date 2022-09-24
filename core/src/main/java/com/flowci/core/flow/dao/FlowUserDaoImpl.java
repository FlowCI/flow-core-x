/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class FlowUserDaoImpl implements FlowUserDao {

    @Autowired
    private MongoOperations mongoOps;

    @Override
    public void create(String flowId) {
        mongoOps.save(new FlowUsers(flowId));
    }

    @Override
    public void delete(String flowId) {
        mongoOps.remove(new FlowUsers(flowId));
    }

    @Override
    public List<String> findAllFlowsByUserEmail(String email) {
        Query q = Query.query(Criteria.where("users").in(email));
        q.fields().exclude("users");

        List<FlowUsers> lists = mongoOps.find(q, FlowUsers.class);
        List<String> ids = new LinkedList<>();

        for (FlowUsers item : lists) {
            ids.add(item.getFlowId());
        }

        return ids;
    }

    @Override
    public List<String> findAllUsers(String flowId) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        FlowUsers flowUsers = mongoOps.findOne(q, FlowUsers.class);

        if (Objects.isNull(flowUsers)) {
            return Collections.emptyList();
        }

        return flowUsers.getUsers();
    }

    @Override
    public void insert(String flowId, Collection<String> emails) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        Update u = new Update().addToSet("users").each(emails);
        mongoOps.upsert(q, u, FlowUsers.class);
    }

    @Override
    public void remove(String flowId, Collection<String> emails) {
        Query q = Query.query(Criteria.where("_id").is(flowId));

        Update u = new Update();
        u.pullAll("users", emails.toArray());

        mongoOps.updateFirst(q, u, FlowUsers.class);
    }

    @Override
    public boolean exist(String flowId, String email) {
        Query q = new Query();
        q.addCriteria(Criteria.where("_id").is(flowId));
        q.addCriteria(Criteria.where("users").is(email));
        return mongoOps.exists(q, FlowUsers.class);
    }
}
