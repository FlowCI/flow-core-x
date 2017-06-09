package com.flow.platform.cc.config;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.mos.MosClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Configuration
public class AppConfig {

    public final static SimpleDateFormat APP_DATE_FORMAT = Jsonable.DOMAIN_DATE_FORMAT;

    public final static Path CMD_LOG_DIR = Paths.get(System.getenv("HOME"), "uploaded-agent-log");

    /**
     * Core Config
     * Enable scheduler task for check idle agent and start instance by zone
     */
    public final static boolean ENABLE_KEEP_IDLE_AGENT_TASK =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.keep_idle_agent", "true"));

    public final static boolean ENABLE_CMD_TIMEOUT_TASK =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.cmd_timeout", "true"));

    private final static int ASYNC_POOL_SIZE = 100;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Value("${agent.config.cmd_log_url}")
    private String cmdLogUrl;

    @Value("${mos.key}")
    private String mosKey;

    @Value("${mos.secret}")
    private String mosSecret;

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
        return new AgentConfig(socketIoUrl, cmdReportUrl, cmdLogUrl);
    }

    @Bean
    public MosClient mosClient() throws Throwable {
        return new MosClient(mosKey, mosSecret);
    }

    @Bean
    public Executor taskExecutor(){
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(ASYNC_POOL_SIZE / 3);
        taskExecutor.setMaxPoolSize(ASYNC_POOL_SIZE);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("async-task");
        return taskExecutor;
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
