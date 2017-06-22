package com.flow.platform.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Created by Will on 17/6/13.
 */
public class CmdDaoImpl extends DaoBase implements CmdDao {
    @Override
    public Cmd find(String cmdId) {
        Cmd cmd = execute(session -> session.get(Cmd.class, cmdId));
        return cmd;
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        List<Cmd> cmds = execute(session -> {
            List<Cmd> cmdList = session.createQuery("from Cmd where AGENT_ZONE = :zone and AGENT_NAME = :name order by CREATED_DATE asc")
                    .setParameter("zone", agentPath.getZone())
                    .setParameter("name", agentPath.getName())
                    .list();
            return cmdList;
        });
        return cmds;
    }

    @Override
    public void baseDelete(String condition) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.createQuery("delete Cmd where ".concat(condition)).executeUpdate();
        tx.commit();
    }

    @Override
    public List<Cmd> listByStatus(String status) {
        List<Cmd> cmds = execute(session -> {
            List<Cmd> cmdList = session.createQuery("from Cmd where STATUS = :status")
                    .setParameter("status", status)
                    .list();
            return cmdList;
        });
        return cmds;
    }

    @Override
    public Cmd findByCmdResultId(String cmdResultId) {
        Cmd cmd = execute(session ->  (Cmd)getSession().createQuery("from Cmd where CMD_RESULT_ID = :cmdReultId")
                .setParameter("cmdResultId", cmdResultId)
                .uniqueResult());
        return cmd;
    }


}
