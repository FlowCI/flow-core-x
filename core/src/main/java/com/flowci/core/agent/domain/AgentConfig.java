package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class AgentConfig {

    private int ExitOnIdle;
}
