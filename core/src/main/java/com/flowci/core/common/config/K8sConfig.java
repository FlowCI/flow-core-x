package com.flowci.core.common.config;

import com.flowci.core.agent.domain.K8sAgentHost;
import com.flowci.docker.domain.Variables;
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

    @Bean("k8sProperties")
    public AppProperties.K8s k8sProperties() {
        AppProperties.K8s props = new AppProperties.K8s();
        props.setNamespace(System.getenv(Variables.NAMESPACE));
        props.setPod(System.getenv(Variables.POD_NAME));
        props.setPodIp(System.getenv(Variables.POD_IP));
        return props;
    }

    @Bean("k8sEndpoints")
    public List<K8sAgentHost.Endpoint> k8sEps() {
        String serverUrl = appProperties.getUrl();
        String rabbitUrl = rabbitProperties.getUri().toString();
        String zkUrl = zkProperties.getHost();

        return K8sAgentHost.buildEndpoints(serverUrl, rabbitUrl, zkUrl);
    }

    @Bean("k8sHosts")
    public K8sAgentHost.Hosts k8sHosts(AppProperties.K8s k8sProperties) {
        String serverUrl = appProperties.getUrl();
        String rabbitUrl = rabbitProperties.getUri().toString();
        String zkUrl = zkProperties.getHost();


        if (k8sProperties.isInCluster()) {
            return new K8sAgentHost.Hosts(serverUrl, rabbitUrl, zkUrl);
        }

        return K8sAgentHost.buildHosts(serverUrl, rabbitUrl, zkUrl);
    }
}
