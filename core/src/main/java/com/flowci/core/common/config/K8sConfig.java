package com.flowci.core.common.config;

import com.flowci.docker.domain.Variables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class K8sConfig {

    @Bean("k8sProperties")
    public AppProperties.K8s k8sProperties() {
        AppProperties.K8s props = new AppProperties.K8s();
        props.setNamespace(System.getenv(Variables.NAMESPACE));
        props.setPod(System.getenv(Variables.POD_NAME));
        props.setPodIp(System.getenv(Variables.POD_IP));
        return props;
    }
}
