package com.flow.platform.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import org.hibernate.Session;

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
            List<Cmd> cmdList = session.createQuery("from Cmd where AGENT_ZONE = :zone and AGENT_NAME = :name")
                    .setParameter("zone", agentPath.getZone())
                    .setParameter("name", agentPath.getName())
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
