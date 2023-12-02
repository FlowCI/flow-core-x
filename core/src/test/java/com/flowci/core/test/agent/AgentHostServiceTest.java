package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.service.AgentHostService;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.common.exception.NotAvailableException;
import com.google.common.collect.Sets;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentHostServiceTest extends ZookeeperScenario {

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentHostService agentHostService;

    @After
    public void cleanup() {
        for (AgentHost h : agentHostService.list()) {
            agentHostService.removeAll(h);
        }
    }

    @Ignore
    @Test(expected = NotAvailableException.class)
    public void should_create_unix_local_host() {
        // when: create host
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        agentHostService.createOrUpdate(host);

        // then:
        Assert.assertNotNull(host.getId());
        Assert.assertEquals(AgentHost.Type.LocalUnixSocket, host.getType());
        Assert.assertEquals(1, agentHostService.list().size());
        Assert.assertEquals(host, agentHostService.list().get(0));

        // when: create other
        AgentHost another = new LocalUnixAgentHost();
        another.setName("test-host-failure");
        another.setTags(Sets.newHashSet("local", "test"));
        agentHostService.createOrUpdate(another);
    }

    @Ignore
    @Test
    public void should_start_agents_on_host() {
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        agentHostService.createOrUpdate(host);

        // when: start agents on host
        Assert.assertTrue(agentHostService.start(host));
        Assert.assertTrue(agentHostService.start(host));
        Assert.assertTrue(agentHostService.start(host));

        // then:
        Assert.assertEquals(3, agentHostService.size(host));
        Assert.assertEquals(3, agentService.list().size());

        agentHostService.removeAll(host);
        Assert.assertEquals(0, agentHostService.size(host));
        Assert.assertEquals(0, agentService.list().size());
    }

    @Ignore
    @Test
    public void should_sync_agents() {
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        agentHostService.createOrUpdate(host);

        // given: create agent, start it and delete host
        Assert.assertTrue(agentHostService.start(host));
        Assert.assertEquals(1, agentHostService.size(host));
        agentHostDao.delete(host);

        // when: create new host, and agent
        host = new LocalUnixAgentHost();
        host.setName("test-host-1");
        agentHostService.createOrUpdate(host);
        Assert.assertTrue(agentHostService.start(host));

        // then:
        agentHostService.sync(host);
        Assert.assertEquals(1, agentHostService.size(host));
    }
}