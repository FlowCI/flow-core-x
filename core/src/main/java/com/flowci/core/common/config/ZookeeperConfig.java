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

package com.flowci.core.common.config;

import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.exception.CIException;
import com.flowci.zookeeper.LocalServer;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class ZookeeperConfig {

    private ZookeeperClient client;

    private LocalServer server;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Bean(name = "zk")
    public ZookeeperClient zookeeperClient(TaskExecutor appTaskExecutor) {
        if (zkProperties.getEmbedded()) {
            startEmbeddedServer();
            log.info("Embedded zookeeper been started ~");
        }

        String host = zkProperties.getHost();
        Integer timeout = zkProperties.getTimeout();
        Integer retry = zkProperties.getRetry();

        client = new ZookeeperClient(host, retry, timeout, appTaskExecutor);
        client.start();

        initRoots(client, zkProperties.getCronRoot());
        initRoots(client, zkProperties.getAgentRoot());

        return client;
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }

        if (server != null) {
            server.stop();
        }
    }

    private void initRoots(ZookeeperClient client, String rootPath) {
        try {
            client.create(CreateMode.PERSISTENT, rootPath, null);
        } catch (ZookeeperException ignore) {

        }
    }

    private void startEmbeddedServer() {
        Path path = Paths.get(zkProperties.getDataDir());
        String address = "0.0.0.0";
        Integer port = 2180;

        ThreadPoolTaskExecutor taskExecutor = ThreadHelper.createTaskExecutor(5, 1, 0, "zk-server-");

        try {
            server = new LocalServer(path, address, port);
            taskExecutor.execute(server);
        } catch (ZookeeperException e) {
            throw new CIException("Unable to start embedded zookeeper: {0}", e.getMessage());
        }
    }
}
