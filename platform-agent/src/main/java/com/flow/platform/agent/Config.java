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

import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.zk.ZKClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.curator.utils.ZKPaths;
import org.apache.http.client.methods.HttpGet;

/**
 * @author gy@fir.im
 */
public class Config {

    public final static String ZK_ROOT = "flow-agents";

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "flow.agent.debug";
    public final static String PROP_CONCURRENT_THREAD = "flow.agent.cmd.thread";
    public final static String PROP_REPORT_STATUS = "flow.agent.cmd.report";

    public final static String PROP_UPLOAD_AGENT_LOG = "flow.agent.log.upload";
    public final static String PROP_ENABLE_REALTIME_AGENT_LOG = "flow.agent.log.realtime";
    public final static String PROP_DEL_AGENT_LOG = "flow.agent.log.delete";
    public final static String PROP_LOG_DIR = "flow.agent.log.dir";

    public final static String PROP_ZK_TIMEOUT = "flow.agent.zk.timeout";
    public final static String PROP_SUDO_PASSWORD = "flow.agent.sudo.pwd";

    public static AgentSettings AGENT_SETTINGS;
    public static String ZK_URL;
    public static String ZONE;
    public static String NAME;

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static int zkTimeout() {
        String intStr = System.getProperty(PROP_ZK_TIMEOUT, "10000"); // default 10 seconds
        return Integer.parseInt(intStr);
    }

    /**
     * Is delete cmd log after uploaded
     */
    public static boolean isDeleteLog() {
        String boolStr = System.getProperty(PROP_DEL_AGENT_LOG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    /**
     * Is upload cmd full load as zip to cc
     */
    public static boolean isUploadLog() {
        String boolStr = System.getProperty(PROP_UPLOAD_AGENT_LOG, "true");
        return Boolean.parseBoolean(boolStr);
    }

    /**
     * Is report cmd status to cc
     */
    public static boolean isReportCmdStatus() {
        String boolStr = System.getProperty(PROP_REPORT_STATUS, "true");
        return Boolean.parseBoolean(boolStr);
    }

    /**
     * Enable to upload real time agent log
     */
    public static boolean enableRealtimeLog() {
        String boolStr = System.getProperty(PROP_ENABLE_REALTIME_AGENT_LOG, "true");
        return Boolean.parseBoolean(boolStr);
    }

    public static Path logDir() {
        Path defaultPath = Paths.get(System.getenv("HOME"), "agent-log");
        String pathStr = System.getProperty(PROP_LOG_DIR, defaultPath.toString());

        try {
            return Paths.get(pathStr);
        } catch (Throwable e) {
            return defaultPath;
        }
    }

    public static int concurrentThreadNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_THREAD, "1");
        return Integer.parseInt(intStr);
    }

    public static String sudoPassword() {
        return System.getProperty(PROP_SUDO_PASSWORD, "");
    }

    public static AgentSettings agentSettings() {
        return AGENT_SETTINGS;
    }

    /**
     * @return zone name
     */
    public static String zone() {
        return ZONE;
    }

    /**
     * @return agent name
     */
    public static String name() {
        return NAME;
    }

    public static String zkUrl() {
        return ZK_URL;
    }

    /**
     * connect to zk server to load config from zone data
     */
    public static AgentSettings loadAgentConfig(String zkHost, String zoneName, int retry) {
        try (ZKClient zkClient = new ZKClient(zkHost, 1000, retry)) {
            zkClient.start();

            String zonePath = ZKPaths.makePath(ZK_ROOT, zoneName);
            byte[] raw = zkClient.getData(zonePath);
            return Jsonable.parse(raw, AgentSettings.class);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static AgentSettings loadAgentConfig(String baseUrl, String token) {
        String url = new StringBuilder(baseUrl)
            .append("/agents/settings")
            .append("?token=")
            .append(token).toString();

        HttpGet httpGet = new HttpGet(url);
        AgentSettings agentSettings = null;

        try {
            String response = ReportManager
                .httpSend(httpGet, 5, "load agent setting success", "get agent setting error");

            agentSettings = Jsonable.parse(response, AgentSettings.class);
        } catch (Throwable throwable) {
            new RuntimeException(String.format("get agent setting error - %s", throwable));
        }

        return agentSettings;
    }
}
