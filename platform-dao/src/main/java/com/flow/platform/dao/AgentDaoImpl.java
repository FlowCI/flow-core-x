package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collection;
import java.util.List;

/**
 * Created by Will on 17/6/12.
 */
public class AgentDaoImpl extends DaoBase implements AgentDao {
    @Override
    public Collection<Agent> onlineList(String zone) {
        Session session = getSession();
        Collection<Agent> agents;
        agents = session.createQuery("from Agent where AGENT_ZONE = :zone and STATUS <> :offline")
                        .setParameter("offline", AgentStatus.OFFLINE.toString())
                        .setParameter("zone", zone)
                        .list();
        session.close();
        return agents;
    }

    @Override
    public List<Agent> onlineList() {
        Session session = getSession();
        List<Agent> agents;
        agents = session.createQuery("from Agent where STATUS <> :offline")
                .setParameter("offline", AgentStatus.OFFLINE.toString())
                .list();
        session.close();
        return agents;
    }

    @Override
    public Agent find(AgentPath agentPath) {
        Session session = getSession();
        Agent agent = (Agent) session.createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name")
                .setParameter("zone", agentPath.getZone())
                .setParameter( "name", agentPath.getName())
                .uniqueResult();
        session.close();
        return agent;
    }

    @Override
    public Agent findOnline(AgentPath agentPath) {
        Session session = getSession();
        Agent agent = (Agent) session.createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name and status <> :offline")
                .setParameter("zone", agentPath.getZone())
                .setParameter( "name", agentPath.getName())
                .setParameter("offline", AgentStatus.OFFLINE)
                .uniqueResult();
        session.close();
        return agent;
    }


    @Override
    public Agent find(String sessionId) {
        Session session = getSession();
        Agent agent = (Agent) session.createQuery("from Agent where sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult();
        return agent;
    }

    @Override
    public Agent findOnline(String sessionId) {
        Session session = getSession();
        Agent agent = (Agent) session.createQuery("from Agent where sessionId = :sessionId and STATUS <> :offline")
                .setParameter("sessionId", sessionId)
                .setParameter("offline", AgentStatus.OFFLINE)
                .uniqueResult();
        return agent;
    }

    @Override
    public List<Agent> findAvailable(String zone) {
        List<Agent> agents = getSession().createQuery("from Agent where AGENT_ZONE = :zone and STATUS = :idle")
                .setParameter("zone", zone)
                .setParameter("idle", AgentStatus.IDLE.toString())
                .list();
        return agents;
    }
}
