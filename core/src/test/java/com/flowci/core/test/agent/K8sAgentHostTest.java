package com.flowci.core.test.agent;

import com.flowci.core.agent.domain.K8sAgentHost;
import org.junit.Test;
import org.junit.Assert;

import java.util.List;

public class K8sAgentHostTest {

    private final String server = "http://192.168.0.123:8080";

    private final String rabbit = "amqp://guest:guest@192.168.0.104:5672";

    private final String zk = "192.168.0.111,192.168.0.111:2180";

    @Test
    public void should_create_endpoint_list() {
        List<K8sAgentHost.Endpoint> endpoints = K8sAgentHost.buildEndpoints(server, rabbit, zk);
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(4, endpoints.size());

        K8sAgentHost.Endpoint serverEp = endpoints.get(0);
        Assert.assertEquals("flow-ci-server", serverEp.getName());
        Assert.assertEquals("192.168.0.123", serverEp.getIp());
        Assert.assertEquals(8080, serverEp.getPort());

        K8sAgentHost.Endpoint rabbitEp = endpoints.get(1);
        Assert.assertEquals("flow-ci-rabbit", rabbitEp.getName());
        Assert.assertEquals("192.168.0.104", rabbitEp.getIp());
        Assert.assertEquals(5672, rabbitEp.getPort());

        K8sAgentHost.Endpoint zkEp0 = endpoints.get(2);
        Assert.assertEquals("flow-ci-zk-0", zkEp0.getName());
        Assert.assertEquals("192.168.0.111", zkEp0.getIp());
        Assert.assertEquals(2181, zkEp0.getPort());

        K8sAgentHost.Endpoint zkEp1 = endpoints.get(3);
        Assert.assertEquals("flow-ci-zk-1", zkEp1.getName());
        Assert.assertEquals("192.168.0.111", zkEp1.getIp());
        Assert.assertEquals(2180, zkEp1.getPort());
    }

    @Test
    public void should_create_k8s_host_link() {
        K8sAgentHost.Hosts hosts = K8sAgentHost.buildHosts(server, rabbit, zk);
        Assert.assertNotNull(hosts);

        Assert.assertEquals("http://flow-ci-server:8080", hosts.getServerUrl());
        Assert.assertEquals("amqp://guest:guest@flow-ci-rabbit:5672", hosts.getRabbitUrl());
        Assert.assertEquals("flow-ci-zk-0:2181,flow-ci-zk-1:2180", hosts.getZkUrl());
    }
}
