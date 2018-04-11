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

import lombok.extern.log4j.Log4j2;

/**
 * @author gy@fir.im
 */
@Log4j2
public class App {

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

        log.trace("========= Flow Agent Started =========");
        log.trace("=== Server: " + baseUrl);
        log.trace("=== Token:  " + token);

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            log.trace("=== Start to load configuration");

            Config.AGENT_SETTINGS = Config.loadAgentConfig(baseUrl, token);
            log.trace("====== Settings: {}", Config.agentSettings());

            Config.ZK_URL = Config.AGENT_SETTINGS.getZookeeperUrl();
            log.trace("====== Zookeeper host: {}", Config.zkUrl());

            Config.ZONE = Config.AGENT_SETTINGS.getAgentPath().getZone();
            log.trace("====== Working zone: {}", Config.zone());

            Config.NAME = Config.AGENT_SETTINGS.getAgentPath().getName();
            log.trace("====== Agent agent: {}", Config.name());

            log.trace("========= Config initialized =========");
        } catch (Throwable e) {
            log.error("Cannot load agent config from zone", e);
            Runtime.getRuntime().exit(1);
        }

        try {
            agentManager = new AgentManager(Config.zkUrl(), Config.zkTimeout(), Config.zone(), Config.name());
            new Thread(agentManager).start();
        } catch (Throwable e) {
            log.error("Got exception when agent running", e);
            Runtime.getRuntime().exit(1);
        }
    }

    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            if (agentManager != null) {
                agentManager.close();
            }

            log.trace("========= Agent end =========");
            log.trace("========= JVM EXIT =========");
        }
    }
}
