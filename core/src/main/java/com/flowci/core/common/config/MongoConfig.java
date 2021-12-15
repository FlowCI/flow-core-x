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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.domain.K8sAgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.domain.SshAgentHost;
import com.flowci.core.common.mongo.EncryptConverter;
import com.flowci.core.common.mongo.VariableMapConverter;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.domain.TextConfig;
import com.flowci.core.job.domain.JobItem;
import com.flowci.core.secret.domain.*;
import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.WebhookTrigger;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableMongoAuditing(auditorAwareRef = "sessionManager")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private MongoProperties mongoProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @NonNull
    @Override
    public MongoClient mongoClient() {
        log.info("Mongo URI: {}", mongoProperties.getUri());
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @NonNull
    @Override
    protected String getDatabaseName() {
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        return Objects.requireNonNull(connectionString.getDatabase());
    }

    @NonNull
    @Override
    @Bean
    public MongoMappingContext mongoMappingContext(MongoCustomConversions customConversions) throws ClassNotFoundException {
        CustomizedMappingContext context = new CustomizedMappingContext();
        context.setInitialEntitySet(getInitialEntitySet());
        context.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
        context.setFieldNamingStrategy(fieldNamingStrategy());
        context.setAutoIndexCreation(true);

        // add addPersistentEntity for subtypes since not registered if called within same thread
        context.addEntity(SmtpConfig.class);
        context.addEntity(TextConfig.class);

        context.addEntity(AuthSecret.class);
        context.addEntity(RSASecret.class);
        context.addEntity(TokenSecret.class);
        context.addEntity(AndroidSign.class);
        context.addEntity(KubeConfigSecret.class);

        context.addEntity(LocalUnixAgentHost.class);
        context.addEntity(SshAgentHost.class);
        context.addEntity(K8sAgentHost.class);

        context.addEntity(EmailTrigger.class);
        context.addEntity(WebhookTrigger.class);

        return context;
    }

    @NonNull
    @Override
    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        VariableMapConverter variableConverter = new VariableMapConverter(objectMapper);
        converters.add(variableConverter.getReader());
        converters.add(variableConverter.getStringVarWriter());
        converters.add(variableConverter.getTypedVarWriter());

        EncryptConverter encryptConverter = new EncryptConverter(appProperties.getSecret());
        converters.add(encryptConverter.new SimpleKeyPairReader());
        converters.add(encryptConverter.new SimpleKeyPairWriter());

        converters.add(encryptConverter.new SimpleAuthPairReader());
        converters.add(encryptConverter.new SimpleAuthPairWriter());

        converters.add(encryptConverter.new SecretFieldReader());
        converters.add(encryptConverter.new SecretFieldWriter());

        converters.add(new JobItem.ContextReader());
        return new MongoCustomConversions(converters);
    }

    private static class CustomizedMappingContext extends MongoMappingContext {
        public void addEntity(Class<?> c) {
            this.addPersistentEntity(c);
        }
    }
}
