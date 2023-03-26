package com.flowci.core.agent.consumer;

import com.flowci.core.agent.event.AgentHostStatusEvent;
import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.manager.SocketPushManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OnAgentHostStatusChange implements ApplicationListener<AgentHostStatusEvent> {

    @Autowired
    private SocketPushManager socketPushManager;

    @Autowired
    private String topicForAgentHost;

    @Override
    public void onApplicationEvent(AgentHostStatusEvent event) {
        socketPushManager.push(topicForAgentHost, PushEvent.STATUS_CHANGE, event.getHost());
    }
}
