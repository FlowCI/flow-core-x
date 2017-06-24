package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import java.util.Collection;
import java.util.List;

/**
 * Created by Will on 17/6/12.
 */
public class AgentDaoImpl extends DaoBase implements AgentDao {

    @Override
    public List<Agent> list(String zone, AgentStatus... status) {
        if (zone == null) {
            throw new IllegalArgumentException("Zone name is required");
        }

        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Agent> criteria = builder.createQuery(Agent.class);

            Root<Agent> root = criteria.from(Agent.class);
            criteria.select(root);

            Predicate whereCriteria = builder.equal(root.get("path").get("zone"), zone);

            if (status != null && status.length > 0) {
                Predicate inStatus = root.get("status").in(status);
                whereCriteria = builder.and(whereCriteria, inStatus);
            }

            // order by created date
            criteria.where(whereCriteria);
            criteria.orderBy(builder.asc(root.get("createdDate")));

            Query<Agent> query = session.createQuery(criteria);
            return query.getResultList();
        });
    }

    @Override
    public void baseDelete(String condition) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.createQuery("delete from Agent where ".concat(condition)).executeUpdate();
        tx.commit();
    }

    @Override
    public Agent find(AgentPath agentPath) {
        Agent agent = execute(session -> (Agent) session.createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name")
            .setParameter("zone", agentPath.getZone())
            .setParameter( "name", agentPath.getName())
            .uniqueResult());
        return agent;
    }

    @Override
    public Agent find(String sessionId) {
        Agent agent = execute(session -> (Agent) session.createQuery("from Agent where sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult());
        return agent;
    }
}
