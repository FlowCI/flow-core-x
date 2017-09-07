/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.cc.domain;

import java.io.IOException;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;

/**
 * @author yh@firim
 */
public class ZkServer extends ZooKeeperServerMain {

    private Boolean isStarted = false;

    public ZkServer() {
        super();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    // stop inner zookeeper server
    public void stop() {
        if (isStarted) {
            shutdown();
        }
    }

    @Override
    public void runFromConfig(ServerConfig config) throws IOException {
        isStarted = true;
        super.runFromConfig(config);
    }
}
