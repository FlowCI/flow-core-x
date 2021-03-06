package com.flowci.core.agent.event;

import com.flowci.core.agent.domain.AgentProfile;
import lombok.Getter;

@Getter
public class OnAgentProfileEvent extends EventFromClient {

    private final AgentProfile profile;

    public OnAgentProfileEvent(Object source, AgentProfile profile) {
        super(source, null, null);
        this.profile = profile;
    }
}
