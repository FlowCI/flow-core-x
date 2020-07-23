package com.flowci.core.common.config;

import com.flowci.pool.DockerClientExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {

    @Bean
    public DockerClientExecutor dockerExecutor() {
        return new DockerClientExecutor();
    }
}
