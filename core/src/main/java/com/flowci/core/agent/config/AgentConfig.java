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

import com.flowci.core.agent.domain.K8sAgentHost;
import com.flowci.core.common.config.AppProperties;
import com.flowci.domain.Settings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class AgentConfig {

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Bean("baseSettings")
    public Settings baseSettings() {
        return createSettings(rabbitProperties.getUri().toString(), zkProperties.getHost());
    }

    @Bean("k8sSettings")
    public Settings k8sSettings(K8sAgentHost.Hosts k8sHosts) {
        return createSettings(k8sHosts.getRabbitUrl(), k8sHosts.getZkUrl());
    }

    private Settings createSettings(String rabbitUri, String zkUrl) {
        Settings.Zookeeper zk = new Settings.Zookeeper();
        zk.setRoot(zkProperties.getAgentRoot());
        zk.setHost(zkUrl);

        Settings.RabbitMQ mq = new Settings.RabbitMQ();
        mq.setUri(rabbitUri);
        mq.setCallback(rabbitProperties.getCallbackQueue());
        mq.setShellLog(rabbitProperties.getShellLogQueue());
        mq.setTtyLog(rabbitProperties.getTtyLogQueue());

        Settings settings = new Settings();
        settings.setZookeeper(zk);
        settings.setQueue(mq);

        log.info(settings);
        return settings;
    }
}
