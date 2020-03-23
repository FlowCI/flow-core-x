package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.service.AgentHostService;
import com.flowci.core.agent.service.AgentHostServiceImpl.AgentItemWrapper;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.Agent;
import com.flowci.exception.NotAvailableException;
import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class AgentHostServiceTest extends ZookeeperScenario {

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentHostService agentHostService;

    @Before
    public void login() {
        mockLogin();
    }

    @After
    public void cleanup() {
        for (AgentHost h : agentHostService.list()) {
            agentHostService.removeAll(h);
        }
    }

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

    @Test
    public void should_should_over_time_limit() {
        AgentHost host = new LocalUnixAgentHost();
        host.setMaxIdleSeconds(1800);
        host.setMaxOfflineSeconds(600);

        // test idle limit
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Assert.assertTrue(host.isOverMaxIdleSeconds(Date.from(updatedAt)));

        updatedAt = Instant.now().minus(2, ChronoUnit.SECONDS);
        Assert.assertFalse(host.isOverMaxIdleSeconds(Date.from(updatedAt)));

        host.setMaxIdleSeconds(AgentHost.NoLimit);
        Assert.assertFalse(host.isOverMaxIdleSeconds(Date.from(updatedAt)));

        // test offline limit
        updatedAt = Instant.now().minus(2, ChronoUnit.HOURS);
        Assert.assertTrue(host.isOverMaxOfflineSeconds(Date.from(updatedAt)));

        updatedAt = Instant.now().minus(5, ChronoUnit.MINUTES);
        Assert.assertFalse(host.isOverMaxOfflineSeconds(Date.from(updatedAt)));

        host.setMaxIdleSeconds(AgentHost.NoLimit);
        Assert.assertFalse(host.isOverMaxOfflineSeconds(Date.from(updatedAt)));
    }

    @Test
    public void should_collect_container() throws InterruptedException {
        // init: create host
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        host.setMaxIdleSeconds(2);
        host.setMaxOfflineSeconds(2);
        agentHostService.createOrUpdate(host);

        // given: two agents up running with idle status
        agentHostService.start(host);
        agentHostService.start(host);

        List<Agent> agents = agentService.list();
        Assert.assertEquals(2, agents.size());
        for (Agent agent : agents) {
            mockAgentOnline(agentService.getPath(agent));
        }

        // when: make sure expired and collect
        ThreadHelper.sleep(3000);
        agentHostService.collect(host);

        // then: container should be stopped, but size should still 2
        Assert.assertEquals(2, agentHostService.size(host));

        // when: do collect again
        for (Agent agent : agents) {
            mockAgentOffline(agentService.getPath(agent));
        }

        ThreadHelper.sleep(3000);
        agentHostService.collect(host);

        // then: container should be removed, and agent removed as well
        Assert.assertEquals(0, agentHostService.size(host));
        Assert.assertEquals(0, agentService.list().size());
    }

    @Test
    public void should_get_containers_that_not_in_agent_list() {
        Set<AgentItemWrapper> agentSet = AgentItemWrapper.toSet(Lists.newArrayList(
                new Agent().setName("local-1"),
                new Agent().setName("local-2")
        ));

        Set<AgentItemWrapper> containerSet = AgentItemWrapper.toSet(Lists.newArrayList(
                AgentContainer.of("1", AgentContainer.name("local-1"), DockerStatus.Running),
                AgentContainer.of("1", AgentContainer.name("local-3"), DockerStatus.Running)
        ));

        containerSet.removeAll(agentSet);
        Assert.assertEquals(1, containerSet.size());
    }

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