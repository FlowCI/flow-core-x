package com.flow.platform.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;

import java.util.List;
import java.util.Set;

/**
 * Created by Will on 17/6/13.
 */
public interface CmdDao extends BaseDao<String, Cmd> {

    /**
     *
     * @param agentPath nullable, zone or agent name also nullable
     * @param types     nullable, select in types
     * @param status    nullable, select in status
     * @return
     */
    List<Cmd> list(AgentPath agentPath, Set<CmdType> types, Set<CmdStatus> status);
}
