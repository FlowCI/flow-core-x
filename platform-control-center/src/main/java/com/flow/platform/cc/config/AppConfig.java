package com.flow.platform.cc.config;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.mos.MosClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import({MosConfig.class})
public class AppConfig {

    public final static SimpleDateFormat APP_DATE_FORMAT = Jsonable.DOMAIN_DATE_FORMAT;

    public final static Path CMD_LOG_DIR = Paths.get(System.getenv("HOME"), "uploaded-agent-log");

    // task toggle for keep idle agent, this task will handle could instance
    public final static boolean TASK_ENABLE_KEEP_IDLE_AGENT =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.agent.keep_idle", "true"));

    // task toggle for check agent session timeout
    public final static boolean TASK_ENABLE_AGENT_SESSION_TIMEOUT =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.agent.session_timeout", "true"));

    // task toggle for check cmd execution timeout
    public final static boolean TASK_ENABLE_CMD_TIMEOUT =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.cmd.timeout", "true"));

    // task toggle for clean mos instance
    public static final boolean TASK_ENABLE_MOS_INSTANCE_CLEAN =
            Boolean.parseBoolean(System.getProperty("flow.cc.task.mos.instance.clean", "true"));

    private final static int ASYNC_POOL_SIZE = 100;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Value("${agent.config.cmd_log_url}")
    private String cmdLogUrl;

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
    public Executor taskExecutor() {
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
