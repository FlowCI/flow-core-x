/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.agent.config;

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables.App;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.domain.Settings;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class AgentConfig {

    @Autowired
    private Environment env;

    @Autowired
    private ConfigProperties.Zookeeper zkProperties;

    @Autowired
    private ConfigProperties.RabbitMQ rabbitProperties;

    @Autowired
    private RabbitQueueOperation callbackQueueManager;

    @Bean("baseSettings")
    public Settings baseSettings() {
        Settings.Zookeeper zk = new Settings.Zookeeper();
        zk.setRoot(zkProperties.getAgentRoot());
        zk.setHost(getZkHost());

        Settings.RabbitMQ mq = new Settings.RabbitMQ();
        mq.setUri(getRabbitUri());
        mq.setCallback(callbackQueueManager.getQueueName());
        mq.setLogsExchange(rabbitProperties.getLoggingExchange());

        Settings settings = new Settings();
        settings.setZookeeper(zk);
        settings.setQueue(mq);

        log.info(settings);
        return settings;
    }

    @Bean("agentHostExecutor")
    public ThreadPoolTaskExecutor agentHostExecutor() {
        return ThreadHelper.createTaskExecutor(10, 1, 10, "agent-host");
    }

    private String getZkHost() {
        return env.getProperty(App.ZookeeperHost, zkProperties.getHost());
    }

    private String getRabbitUri() {
        String uri = rabbitProperties.getUri().toString();
        String domain = env.getProperty(App.RabbitHost);

        if (Objects.isNull(domain)) {
            return uri;
        }

        return uri.replace(rabbitProperties.getUri().getHost(), domain);
    }
}
