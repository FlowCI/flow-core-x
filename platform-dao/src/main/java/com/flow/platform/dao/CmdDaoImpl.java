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
        Cmd cmd = getSession().get(Cmd.class, cmdId);
        return cmd;
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        List<Cmd> cmds = getSession().createQuery("from Cmd where agentZone = :zone and agentName = :name")
                .setParameter("zone", agentPath.getZone())
                .setParameter("name", agentPath.getName())
                .list();
        return cmds;
    }
}
