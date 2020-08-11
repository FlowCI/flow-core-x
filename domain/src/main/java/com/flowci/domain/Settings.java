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

package com.flowci.domain;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings to sync from server to agent
 *
 * @author yang
 */
@Data
@NoArgsConstructor
public class Settings implements Serializable {

    private Agent agent;

    private RabbitMQ queue;

    private Zookeeper zookeeper;

    public Settings(Agent agent, RabbitMQ queue, Zookeeper zookeeper) {
        this.agent = agent;
        this.queue = queue;
        this.zookeeper = zookeeper;
    }

    @Data
    public static class Zookeeper implements Serializable {

        private String host;

        private String root;
    }

    @Data
    public static class RabbitMQ implements Serializable {

        private String uri;

        // queue name for step callback
        private String callback;

        // queue name for shell log
        private String shellLog;

        // queue name for tty log
        private String ttyLog;
    }

}
