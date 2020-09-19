package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "agent_host")
public class K8sAgentHost extends AgentHost {

    @NonNull
    private String namespace;

    @NonNull
    private String secret; // secret for config file

    public K8sAgentHost() {
        setType(Type.K8s);
    }
}
