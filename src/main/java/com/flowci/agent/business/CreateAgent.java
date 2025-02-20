package com.flowci.agent.business;

import com.flowci.agent.model.Agent;

import java.util.List;

public interface CreateAgent {
    Agent invoke(String alias, List<String> tags);
}
