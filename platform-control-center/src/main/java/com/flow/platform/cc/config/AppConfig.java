package com.flow.platform.cc.config;

import com.flow.platform.domain.AgentConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public class AppConfig {

    private static final int ASYNC_POOL_SIZE = 100;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Bean
    public AgentConfig agentConfig() {
        return new AgentConfig(socketIoUrl, cmdReportUrl);
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(ASYNC_POOL_SIZE, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
    }
}
