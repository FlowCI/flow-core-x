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

package com.flowci.zookeeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

/**
 * @author yang
 */
public class LocalServer extends ZooKeeperServerMain implements Runnable {

    private Boolean isStarted = false;

    private final Properties properties = new Properties();

    public LocalServer(Path dataDir, String address, Integer port) {
        super();

        properties.setProperty("dataDir", dataDir.toString());
        properties.setProperty("clientPort", port.toString());
        properties.setProperty("clientPortAddress", address);
        properties.setProperty("tickTime", "1500");
        properties.setProperty("maxClientCnxns", "50");
    }

    public void run() {
        try {
            QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
            ServerConfig configuration = new ServerConfig();

            quorumPeerConfig.parseProperties(properties);
            configuration.readFrom(quorumPeerConfig);

            this.runFromConfig(configuration);
        } catch (Exception e) {
            throw new ZookeeperException("Unable to start embedded zookeeper server: {}", e.getMessage());
        }
    }

    // stop inner zookeeper server
    public void stop() {
        if (isStarted) {
            shutdown();
        }
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    @Override
    public void runFromConfig(ServerConfig config) throws IOException, AdminServer.AdminServerException {
        isStarted = true;
        super.runFromConfig(config);
    }
}
