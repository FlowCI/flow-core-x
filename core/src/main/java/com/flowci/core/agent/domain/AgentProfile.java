package com.flowci.core.agent.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Id is agent token
 */
@Getter
@Setter
@Accessors(chain = true)
public class AgentProfile extends Mongoable {

    private int cpuNum;

    private double cpuUsage;

    private int totalMemory; // in MB

    private int freeMemory; // in MB

    private int totalDisk; // in MB

    private int freeDisk; // in MB
}
