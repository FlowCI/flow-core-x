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

package com.flow.platform.agent;

import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import java.io.IOException;

/**
 * @author gy@fir.im
 */
public class App {

    private final static Logger LOGGER = new Logger(App.class);

    private static AgentManager agentManager;

    public static void main(String args[]) {

        String baseUrl = null;
        String token = null;

        if (args.length != 2) {
            System.out.println("Missing arguments: please specify api host, agent token");
            System.out.println("Cmd: java -jar {api baseUrl} {api token}");
            Runtime.getRuntime().exit(1);
        } else {
            baseUrl = args[0];
            token = args[1];
        }

        LOGGER.trace("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            LOGGER.trace("========= Init config =========");

            Config.AGENT_SETTINGS = Config.loadAgentConfig(baseUrl, token);
            LOGGER.trace(" -- Settings: %s", Config.agentSettings());

            Config.ZK_URL = Config.AGENT_SETTINGS.getZookeeperUrl();
            LOGGER.trace(" -- Zookeeper host: %s", Config.zkUrl());

            Config.ZONE = Config.AGENT_SETTINGS.getAgentPath().getZone();
            LOGGER.trace(" -- Working zone: %s", Config.zone());

            Config.NAME = Config.AGENT_SETTINGS.getAgentPath().getName();
            LOGGER.trace(" -- Agent agent: %s", Config.name());

            LOGGER.trace("========= Config initialized =========");
        } catch (Throwable e) {
            LOGGER.error("Cannot load agent config from zone", e);
            Runtime.getRuntime().exit(1);
        }

        try {
            agentManager = new AgentManager(Config.zkUrl(), Config.zkTimeout(), Config.zone(), Config.name());
            new Thread(agentManager).start();
        } catch (Throwable e) {
            LOGGER.error("Got exception when agent running", e);
            Runtime.getRuntime().exit(1);
        }
    }

    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            if (agentManager != null) {
                try {
                    agentManager.close();
                } catch (IOException ignore) {

                }
            }

            LOGGER.trace("========= Agent end =========");
            LOGGER.trace("========= JVM EXIT =========");
        }
    }
}
