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

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author yh@firim
 */
@Configuration
@PropertySource("classpath:app-default.properties")
public class MQConfig {

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.exchange}")
    private String exchangeName;

    private Connection connection;
    private Channel channel;

    private Connection createConnection(){
        ConnectionFactory factory = new ConnectionFactory();
        Connection conn = null;
        try {
            factory.setUri(host);
            conn = factory.newConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return conn;
    }

    private Channel createChannel() throws IOException {
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT);
        return channel;
    }

    @PostConstruct
    public void init(){
        try {
            connection = createConnection();
            channel = createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Bean(name = "jobChannel")
    public Channel jobChannel(){
        return channel;
    }

    @PreDestroy
    public void destroy() {
        try {
            connection.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
