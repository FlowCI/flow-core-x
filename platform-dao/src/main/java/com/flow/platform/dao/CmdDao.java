package com.flow.platform.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;

import java.util.List;

/**
 * Created by Will on 17/6/13.
 */
public interface CmdDao extends BaseDao<String, Cmd> {

    Cmd find(String cmdId);

    List<Cmd> listByAgentPath(AgentPath agentPath);

    Cmd findByCmdResultId(String cmdResultId);

    List<Cmd> listByStatus(String status);

    void baseDelete(String condition);
}
