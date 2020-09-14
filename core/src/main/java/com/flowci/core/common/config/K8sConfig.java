package com.flowci.core.common.config;

import com.flowci.core.agent.domain.K8sAgentHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class K8sConfig {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Bean("k8sHosts")
    public K8sAgentHost.Hosts k8sHosts() {
        String serverUrl = appProperties.getUrl();
        String rabbitUrl = rabbitProperties.getUri().toString();
        String zkUrl = zkProperties.getHost();
        return K8sAgentHost.buildHosts(serverUrl, rabbitUrl, zkUrl);
    }

    @Bean("k8sEndpoints")
    public List<K8sAgentHost.Endpoint> k8sEps() {
        String serverUrl = appProperties.getUrl();
        String rabbitUrl = rabbitProperties.getUri().toString();
        String zkUrl = zkProperties.getHost();

        return K8sAgentHost.buildEndpoints(serverUrl, rabbitUrl, zkUrl);
    }
}
