package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.service.AgentHostService;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.common.exception.NotAvailableException;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class AgentHostServiceTest extends ZookeeperScenario {

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentHostService agentHostService;

    @AfterEach
    void  cleanup() {
        for (AgentHost h : agentHostService.list()) {
            agentHostService.removeAll(h);
        }
    }

    @Disabled
    @Test
    void  should_create_unix_local_host() {
        // when: create host
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        agentHostService.createOrUpdate(host);

        // then:
        assertNotNull(host.getId());
        assertEquals(AgentHost.Type.LocalUnixSocket, host.getType());
        assertEquals(1, agentHostService.list().size());
        assertEquals(host, agentHostService.list().get(0));

        // when: create other
        assertThrows(NotAvailableException.class, () -> {
            AgentHost another = new LocalUnixAgentHost();
            another.setName("test-host-failure");
            another.setTags(Sets.newHashSet("local", "test"));
            agentHostService.createOrUpdate(another);
        });
    }

    @Disabled
    @Test
    void  should_start_agents_on_host() {
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        agentHostService.createOrUpdate(host);

        // when: start agents on host
        assertTrue(agentHostService.start(host));
        assertTrue(agentHostService.start(host));
        assertTrue(agentHostService.start(host));

        // then:
        assertEquals(3, agentHostService.size(host));
        assertEquals(3, agentService.list().size());

        agentHostService.removeAll(host);
        assertEquals(0, agentHostService.size(host));
        assertEquals(0, agentService.list().size());
    }

    @Disabled
    @Test
    void  should_sync_agents() {
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        agentHostService.createOrUpdate(host);

        // given: create agent, start it and delete host
        assertTrue(agentHostService.start(host));
        assertEquals(1, agentHostService.size(host));
        agentHostDao.delete(host);

        // when: create new host, and agent
        host = new LocalUnixAgentHost();
        host.setName("test-host-1");
        agentHostService.createOrUpdate(host);
        assertTrue(agentHostService.start(host));

        // then:
        agentHostService.sync(host);
        assertEquals(1, agentHostService.size(host));
    }
}