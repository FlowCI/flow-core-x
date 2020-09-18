package com.flowci.core.common.health;

import com.flowci.core.agent.domain.K8sAgentHost;
import com.flowci.core.common.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class K8sCheck extends AbstractHealthIndicator {

    @Autowired
    private AppProperties.K8s k8sProperties;

    @Autowired
    private K8sAgentHost.Hosts k8sHosts;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Status status = k8sProperties.isInCluster() ? Status.UP : Status.DOWN;
        builder.status(status);
        if (status == Status.UP) {
            builder.withDetail("namespace", k8sProperties.getNamespace())
                    .withDetail("pod", k8sProperties.getPod())
                    .withDetail("pod ip", k8sProperties.getPodIp());
        }

        builder.withDetail("server", k8sHosts.getServerUrl());
        builder.withDetail("rabbit", k8sHosts.getRabbitUrl());
        builder.withDetail("zk", k8sHosts.getZkUrl());
    }
}
