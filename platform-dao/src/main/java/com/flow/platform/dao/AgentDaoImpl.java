package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.google.common.collect.Sets;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

/**
 * Created by Will on 17/6/12.
 */
@Repository
public class AgentDaoImpl extends AbstractBaseDao<AgentPath, Agent> implements AgentDao {

    private final Set<String> orderByFields = Sets.newHashSet("createdDate", "updatedDate", "sessionDate");

    @Override
    Class getEntityClass() {
        return Agent.class;
    }

    @Override
    public List<Agent> list(String zone, String orderByField, AgentStatus... status) {
        if (zone == null) {
            throw new IllegalArgumentException("Zone name is required");
        }

        if (orderByField != null && !orderByFields.contains(orderByField)) {
            throw new IllegalArgumentException("The orderByField only availabe among 'createdDate', 'updateDate' or 'sessionDate'");
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
    public void baseDelete(String condition) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            session.createQuery("delete from Agent where ".concat(condition)).executeUpdate();
            tx.commit();
        }
    }

    @Override
    public Agent find(AgentPath agentPath) {
        Agent agent = (Agent) execute(session -> (Agent) session.createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name")
            .setParameter("zone", agentPath.getZone())
            .setParameter( "name", agentPath.getName())
            .uniqueResult());
        return agent;
    }

    @Override
    public Agent find(String sessionId) {
        Agent agent = (Agent) execute(session -> (Agent) session.createQuery("from Agent where sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult());
        return agent;
    }
}
