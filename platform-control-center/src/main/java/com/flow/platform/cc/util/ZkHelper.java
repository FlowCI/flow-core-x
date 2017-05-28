package com.flow.platform.cc.util;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by gy@fir.im on 28/05/2017.
 * Copyright fir.im
 */
@Component(value = "zkHelper")
public class ZkHelper {

    @Value("${zk.node.root}")
    private String zkRootName;

    /**
     * Get zk path builder for agent
     *
     * @param zone zone name
     * @param name agent name
     * @return
     */
    public ZkPathBuilder buildZkPath(String zone, String name) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        if (zone != null) {
            pathBuilder.append(zone);
            if (name != null) {
                pathBuilder.append(name);
            }
        }
        return pathBuilder;
    }

    /**
     * Get zookeeper path from AgentPath object
     *
     * @param agentPath
     * @return
     */
    public String getZkPath(AgentPath agentPath) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        pathBuilder.append(agentPath.getZone()).append(agentPath.getName());
        return pathBuilder.path();
    }

    /**
     * Get zookeeper path from CmdBase object
     *
     * @param cmd
     * @return
     */
    public String getZkPath(CmdBase cmd) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        pathBuilder.append(cmd.getZone()).append(cmd.getAgent());
        return pathBuilder.path();
    }
}
