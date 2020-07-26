package com.flowci.core.common.config;

import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSDKManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {

    @Bean
    public DockerManager dockerManager() {
        return new DockerSDKManager(DockerManager.DockerLocalHost);
    }
}
