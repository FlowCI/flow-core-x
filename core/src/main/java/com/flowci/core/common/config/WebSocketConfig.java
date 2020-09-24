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

package com.flowci.core.common.config;

import com.flowci.core.agent.manager.AgentEventManager;
import com.flowci.core.common.helper.ThreadHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @author yang
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    @Autowired
    private AgentEventManager agentEventManager;

    /**
     * To subscribe git test status update for flow
     * Ex: /topic/flows/git/test/{flow id}
     */
    @Bean("topicForGitTest")
    public String topicForGitTest() {
        return "/topic/flows/git/test";
    }

    /**
     * To subscribe new job
     */
    @Bean("topicForJobs")
    public String topicForJobs() {
        return "/topic/jobs";
    }

    /**
     * To subscribe step update for job
     * Ex: /topic/steps/{job id}
     */
    @Bean("topicForSteps")
    public String topicForSteps() {
        return "/topic/steps";
    }

    /**
     * To subscribe tty action
     * Ex: /topic/tty/action/{job id}
     */
    @Bean("topicForTtyAction")
    public String topicForTtyAction() {
        return "/topic/tty/action";
    }

    /**
     * To subscribe tty logs
     * Ex: /topic/tty/logs/{job id}
     */
    @Bean("topicForTtyLogs")
    public String topicForTtyLogs() {
        return "/topic/tty/logs";
    }

    /**
     * To subscribe task update for job
     * Ex: /topic/tasks/{job id}
     */
    @Bean("topicForTasks")
    public String topicForTasks() {
        return "/topic/tasks";
    }

    /**
     * To subscribe real time logging for all jobs.
     * Ex: /topic/logs/{job id}
     */
    @Bean("topicForLogs")
    public String topicForLogs() {
        return "/topic/logs";
    }

    /**
     * To subscribe agent update
     */
    @Bean("topicForAgents")
    public String topicForAgents() {
        return "/topic/agents";
    }

    /**
     * To subscribe agent host update
     */
    @Bean("topicForAgentHost")
    public String topicForAgentHost() {
        return "/topic/hosts";
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(10, 5, 10, "ws-inbound-");
        registration.taskExecutor(executor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(10, 5, 10, "ws-outbound-");
        registration.taskExecutor(executor);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentEventManager, "/agent").setAllowedOrigins("*");
    }
}
