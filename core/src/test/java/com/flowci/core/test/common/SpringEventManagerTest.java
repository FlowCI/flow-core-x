package com.flowci.core.test.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.event.AgentIdleEvent;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.Agent;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.testng.Assert;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SpringEventManagerTest extends SpringScenario {

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void should_convert_to_json() throws IOException {
        Agent mock = new Agent();
        mock.setId("123");
        mock.setStatus(Agent.Status.BUSY);

        AgentIdleEvent event = new AgentIdleEvent(this, mock);

        byte[] bytes = objectMapper.writeValueAsBytes(event);
        Assert.assertNotNull(bytes);
        Assert.assertTrue(bytes.length > 0);

        AgentIdleEvent decoded = objectMapper.readValue(bytes, AgentIdleEvent.class);
        Assert.assertNotNull(decoded);
        Assert.assertEquals(mock.getId(), decoded.getAgent().getId());
        Assert.assertEquals(mock.getStatus(), decoded.getAgent().getStatus());
    }

    @Test
    public void should_send_and_receive_broadcast_event() throws InterruptedException {
        Agent mock = new Agent();
        mock.setId("123");
        mock.setStatus(Agent.Status.BUSY);

        CountDownLatch latch = new CountDownLatch(1);
        addEventListener((ApplicationListener<AgentIdleEvent>) event -> {
            Assert.assertTrue(event.isInternal());
            latch.countDown();
        });

        AgentIdleEvent event = new AgentIdleEvent(this, mock);
        eventManager.publish(event);

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
