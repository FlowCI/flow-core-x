package com.flow.platform.domain;

/**
 * Used for send cmd info
 *
 * Created by gy@fir.im on 22/06/2017.
 * Copyright fir.im
 */
public class CmdInfo extends CmdBase {

    public CmdInfo(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public CmdInfo(AgentPath agentPath, CmdType type, String cmd) {
        super(agentPath, type, cmd);
    }
}
