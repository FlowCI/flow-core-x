/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.dao;

import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.DateUtil;
import com.google.common.collect.Sets;
import java.util.HashSet;
import javax.persistence.criteria.CriteriaUpdate;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Set;

/**
 * @author Will
 */
@Repository(value = "agentDao")
public class AgentDaoImpl extends AbstractBaseDao<AgentPath, Agent> implements AgentDao {

    private final Set<String> orderByFields = Sets
        .newHashSet("createdDate", "updatedDate", "sessionDate");

    @Override
    protected Class getEntityClass() {
        return Agent.class;
    }

    @Override
    protected String getKeyName() {
        return "path";
    }

    @Override
    public List<Agent> list(Set<AgentPath> keySet) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();

            CriteriaQuery<Agent> select = builder.createQuery(Agent.class);
            Root<Agent> from = select.from(Agent.class);

            Set<String> zones = new HashSet<>(keySet.size());
            Set<String> agents = new HashSet<>(keySet.size());

            for (AgentPath path : keySet) {
                if (path.getZone() != null) {
                    zones.add(path.getZone());
                }

                if (path.getName() != null) {
                    agents.add(path.getName());
                }
            }

            Predicate zoneInClause = from.get(getKeyName()).get("zone").in(zones);
            Predicate nameInClause = from.get(getKeyName()).get("name").in(agents);
            select.where(builder.and(zoneInClause, nameInClause));

            return session.createQuery(select).list();
        });
    }

    @Override
    public void update(final Agent obj) {
        execute(session -> {
            obj.setUpdatedDate(DateUtil.now());
            session.update(obj);
            return null;
        });
    }

    @Override
    public List<Agent> list(final String zone, final String orderByField, final AgentStatus... status) {
        if (zone == null) {
            throw new IllegalArgumentException("Zone name is required");
        }

        if (orderByField != null && !orderByFields.contains(orderByField)) {
            throw new IllegalArgumentException(
                "The orderByField only availabe among 'createdDate', 'updateDate' or 'sessionDate'");
        }

        return (List<Agent>) execute(new Executable() {
            @Override
            public Object execute(Session session) {
                CriteriaBuilder builder = session.getCriteriaBuilder();
                CriteriaQuery<Agent> criteria = builder.createQuery(Agent.class);

                Root<Agent> root = criteria.from(Agent.class);
                criteria.select(root);

                Predicate whereCriteria = builder.equal(root.get("path").get("zone"), zone);

                if (status != null && status.length > 0) {
                    Predicate inStatus = root.get("status").in(status);
                    whereCriteria = builder.and(whereCriteria, inStatus);
                }
                criteria.where(whereCriteria);

                // order by created date
                if (orderByField != null) {
                    criteria.orderBy(builder.asc(root.get(orderByField)));
                }

                Query<Agent> query = session.createQuery(criteria);
                return query.getResultList();
            }
        });
    }

    @Override
    public Agent find(final AgentPath agentPath) {
        // TODO: AgentPath is key, should overwrite get method at least

        Agent agent = (Agent) execute(session -> (Agent) session
            .createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name")
            .setParameter("zone", agentPath.getZone())
            .setParameter("name", agentPath.getName())
            .uniqueResult());
        return agent;
    }

    @Override
    public Agent find(final String sessionId) {
        Agent agent = (Agent) execute(
            session -> (Agent) session.createQuery("from Agent where sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult());
        return agent;
    }

    @Override
    public int batchUpdateStatus(String zone, AgentStatus status, Set<String> agents, boolean isNot) {

        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaUpdate<Agent> criteria = builder.createCriteriaUpdate(Agent.class);

            Root<Agent> from = criteria.from(Agent.class);
            criteria.set(from.get("status"), status);

            Predicate whereClause = builder.equal(from.get("path").get("zone"), zone);

            if (agents == null || agents.size() == 0) {
                return session.createQuery(criteria).executeUpdate();
            }

            // set agents to where clause
            Predicate inAgents;
            if (isNot) {
                inAgents = builder.not(from.get("path").get("name").in(agents));
            } else {
                inAgents = from.get("path").get("name").in(agents);
            }

            criteria.where(builder.and(whereClause, inAgents));
            return session.createQuery(criteria).executeUpdate();
        });
    }
}
