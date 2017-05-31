package com.flow.platform.cc.config;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Jsonable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public class AppConfig {

    public final static SimpleDateFormat APP_DATE_FORMAT = Jsonable.DOMAIN_DATE_FORMAT;

    public final static Path CMD_LOG_DIR = Paths.get(System.getenv("HOME"), "uploaded-agent-log");

    private final static int ASYNC_POOL_SIZE = 100;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(CMD_LOG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    /**
     * Add cmd log file path to queue for processing
     *
     * @return BlockingQueue with file path
     */
    @Bean
    public Queue<Path> cmdLoggingQueue() {
        return new ConcurrentLinkedQueue<>();
    }
}
