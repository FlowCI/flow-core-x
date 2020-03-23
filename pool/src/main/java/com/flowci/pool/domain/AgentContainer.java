package com.flowci.pool.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
public class AgentContainer {

    public static final String Image = "flowci/agent:latest";

    public static final String Prefix = "ci-agent";

    public static final String NameFilter = Prefix + "*";

    public static String name(String agentName) {
        return String.format("%s.%s", Prefix, agentName);
    }

    private final String id;

    private final String name;

    private final String state;

    public String getAgentName() {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            throw new IllegalArgumentException("Cannot get agent name from container name");
        }
        return name.substring(index + 1);
    }
}