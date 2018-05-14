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
import com.flow.platform.util.http.HttpURL;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class AgentConfig {

    @Value("${agent.config.ws}")
    private String wsDomain; // web socket url

    @Value("${agent.config.cc}")
    private String ccDomain; // control-center url

    @Value("${zk.host}")
    private String zookeeperUrl;

    private final AgentSettings settings = new AgentSettings();

    @PostConstruct
    public void init() {
        final String webSocketUrl = HttpURL.build(wsDomain).append("agent/cmd/logging").toString();
        settings.setWebSocketUrl(webSocketUrl);

        final String cmdStatusUrl = HttpURL.build(ccDomain).append("cmd/report").toString();
        settings.setCmdStatusUrl(cmdStatusUrl);

        final String cmdLogUploadUrl = HttpURL.build(ccDomain).append("cmd/log/upload").toString();
        settings.setCmdLogUrl(cmdLogUploadUrl);

        settings.setZookeeperUrl(zookeeperUrl);
        log.trace(settings.toString());
    }

    @Bean
    public AgentSettings agentSettings() {
        return settings;
    }
}
