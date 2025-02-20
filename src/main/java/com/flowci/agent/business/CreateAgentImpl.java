package com.flowci.agent.business;

import com.flowci.agent.model.Agent;
import com.flowci.agent.repo.AgentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAgentImpl implements CreateAgent {

    private final AgentRepo agentRepo;

    @Override
    public Agent invoke(String alias, List<String> tags) {
        var agent = new Agent();
        agent.setAlias(alias);
        agent.setTags(tags.toArray(new String[0]));
        return agentRepo.save(agent);
    }
}
