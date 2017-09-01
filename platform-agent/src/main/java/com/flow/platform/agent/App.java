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

/**
 * @author gy@fir.im
 */
public class App {

    private final static Logger LOGGER = new Logger(App.class);

    public static void main(String args[]) {
        String zkHome = null; // zookeeper address
        String zone = null; // agent zone
        String name = null; // agent name

        if (args.length != 3) {
            System.out.println("Missing arguments: please specify zookeeper host, working zone and agent name");
            System.out.println("Cmd: java -jar {zookeeper host} {working zone} {agent name}");
            Runtime.getRuntime().exit(1);
        } else {
            zkHome = args[0];
            zone = args[1];
            name = args[2];
        }

        LOGGER.trace("========= Run agent =========");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            LOGGER.trace("========= Init config =========");

            Config.AGENT_SETTINGS = Config.loadAgentConfig(zkHome, zone, 5);
            LOGGER.trace(" -- Settings: %s", Config.agentSettings());

            Config.ZK_URL = zkHome;
            LOGGER.trace(" -- Zookeeper host: %s", Config.zkUrl());

            Config.ZONE = zone;
            LOGGER.trace(" -- Working zone: %s", Config.zone());

            Config.NAME = name;
            LOGGER.trace(" -- Agent agent: %s", Config.name());

            LOGGER.trace("========= Config initialized =========");
        } catch (Throwable e) {
            LOGGER.error("Cannot load agent config from zone", e);
            Runtime.getRuntime().exit(1);
        }

        try {
            AgentManager client = new AgentManager(zkHome, Config.zkTimeout(), zone, name);
            new Thread(client).start();
        } catch (Throwable e) {
            LOGGER.error("Got exception when agent running", e);
            Runtime.getRuntime().exit(1);
        }
    }

    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            LOGGER.trace("========= Agent end =========");
            LOGGER.trace("========= JVM EXIT =========");
        }
    }
}
