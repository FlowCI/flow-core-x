package com.flowci.pool.test;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;

import org.junit.Assert;
import org.junit.Test;


public class AgentContainerTest {

    @Test
    public void should_get_container_name() {
        String name = AgentContainer.name("hello");
        Assert.assertEquals("ci-agent.hello", name);
    }

    @Test
    public void should_get_agent_name_from_container_name() {
        AgentContainer instance = AgentContainer.of("id", "ci-agent.hello", DockerStatus.Exited);
        Assert.assertEquals("hello", instance.getAgentName());
    }

}