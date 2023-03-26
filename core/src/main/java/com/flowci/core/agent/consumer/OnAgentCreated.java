package com.flowci.core.agent.consumer;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.event.AgentCreatedEvent;
import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.manager.SocketPushManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OnAgentCreated implements ApplicationListener<AgentCreatedEvent> {

    @Autowired
    private SocketPushManager socketPushManager;

    @Autowired
    private String topicForAgents;

    @Override
    public void onApplicationEvent(AgentCreatedEvent event) {
        Agent agent = event.getAgent();
        socketPushManager.push(topicForAgents, PushEvent.NEW_CREATED, agent);
    }
}
