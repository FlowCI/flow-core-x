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

package com.flow.platform.api.config;

import com.flow.platform.api.consumer.CmdLoggingConsumer;
import com.flow.platform.api.consumer.JobStatusEventPushHandler;
import com.flow.platform.api.consumer.NodeStatusEventPushHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author yang
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer implements WebSocketConfigurer {

    // for agent uploading real time log
    private final static String URL_FOR_AGENT_CMD_LOGGING = "/agent/cmd/logging";

    // for cmd logging which web connected to
    private final static String URL_FOR_FOR_WEB_CONNECTION = "/ws/web";

    public final static String TOPIC_FOR_JOB = "/topic/job";

    public final static String TOPIC_FOR_CMD = "/topic/cmd";

    @Bean
    public WebSocketHandler cmdLoggingConsumer() {
        return new CmdLoggingConsumer();
    }

    @Bean
    public JobStatusEventPushHandler jobEventHandler() {
        return new JobStatusEventPushHandler();
    }

    @Bean
    public NodeStatusEventPushHandler nodeEventHandler() {
        return new NodeStatusEventPushHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketHandler webSocketHandler = cmdLoggingConsumer();
        registry.addHandler(webSocketHandler, URL_FOR_AGENT_CMD_LOGGING).setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(TOPIC_FOR_JOB, TOPIC_FOR_CMD);
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(URL_FOR_FOR_WEB_CONNECTION)
                    .setAllowedOrigins("*")
                    .withSockJS();
    }
}
