package com.flowci.core.test.agent;

import com.flowci.core.agent.domain.K8sAgentHost;
import org.junit.Test;
import org.junit.Assert;

import java.util.List;

public class K8sAgentHostTest {

    @Test
    public void should_create_endpoint_list() {
        String server = "http://192.168.0.123:8080";
        String rabbit = "amqp://guest:guest@192.168.0.104:5672";
        String zk = "192.168.0.111";

        List<K8sAgentHost.Endpoint> endpoints = K8sAgentHost.buildEndpoints(server, rabbit, zk);
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(3, endpoints.size());

        K8sAgentHost.Endpoint serverEp = endpoints.get(0);
        Assert.assertEquals("flow-ci-server", serverEp.getName());
        Assert.assertEquals("192.168.0.123", serverEp.getIp());
        Assert.assertEquals(8080, serverEp.getPort());

        K8sAgentHost.Endpoint rabbitEp = endpoints.get(1);
        Assert.assertEquals("flow-ci-rabbit", rabbitEp.getName());
        Assert.assertEquals("192.168.0.104", rabbitEp.getIp());
        Assert.assertEquals(5672, rabbitEp.getPort());

        K8sAgentHost.Endpoint zkEp = endpoints.get(2);
        Assert.assertEquals("flow-ci-zk-0", zkEp.getName());
        Assert.assertEquals("192.168.0.111", zkEp.getIp());
        Assert.assertEquals(2181, zkEp.getPort());
    }
}
