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

package com.flow.platform.cc.config;

import com.flow.platform.domain.AgentSettings;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Configuration
public class AgentConfig {

    private final static Logger LOGGER = new Logger(AgentConfig.class);

    @Value("${agent.config.cmd_rt_log_url}")
    private String cmdRtLogUrl; // web socket url

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Value("${agent.config.cmd_log_url}")
    private String cmdLogUrl;

    @Value("${zk.host}")
    private String zookeeperUrl;

    @PostConstruct
    public void init() {
        LOGGER.trace("Real time log ws url: %s", cmdRtLogUrl);
        LOGGER.trace("Report cmd status url: %s", cmdReportUrl);
        LOGGER.trace("Upload cmd zip log url: %s", cmdLogUrl);
        LOGGER.trace("zookeeper url: %s", zookeeperUrl);
    }

    @Bean
    public AgentSettings agentSettings() {
        return new AgentSettings(cmdRtLogUrl, cmdReportUrl, cmdLogUrl, zookeeperUrl);
    }
}
