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

package com.flow.platform.cc.service;

import com.flow.platform.util.zk.ZKClient;
import com.flow.platform.util.zk.ZKServer;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class ZooKeeperServiceImpl implements ZooKeeperService {

    @Autowired
    private ZKClient client;

    @Autowired
    private ZKServer zkServer;

    @Override
    public void start() {
        // not start since ZKClient been started in zookeeper config
    }

    @Override
    public void stop() {
        try {
            client.close();
            if (zkServer != null) {
                zkServer.stop();
            }
        } catch (IOException e) {
            log.warn("Fail to close zk client connection: {}", e.getMessage());
        }
    }
}
