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

package com.flowci.core.test;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.api.domain.SimpleUser;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.common.service.SettingService;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.*;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.job.domain.*;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.test.SpringScenario.Config;
import com.flowci.core.test.auth.AuthHelper;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.TriggerDelivery;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Config.class,
        properties = {"spring.main.allow-bean-definition-overriding=true"}
)

@AutoConfigureMockMvc
public abstract class SpringScenario {

    @TestConfiguration
    public static class Config {

        @Bean("mvcMockHelper")
        public MockMvcHelper mvcMockHelper() {
            return new MockMvcHelper();
        }

        @Bean("flowMockHelper")
        public FlowMockHelper flowMockHelper() {
            return new FlowMockHelper();
        }

        @Bean
        public AuthHelper authHelper() {
            return new AuthHelper();
        }

        /**
         * Rewrite the web mvc config to ignore jackson mxis
         */
        @Bean
        public Map<Class<?>, Class<?>> mixins() {
            return Collections.emptyMap();
        }

        @Bean("templates")
        public List<Template> getTemplates() {
            return Lists.emptyList();
        }
    }

    @MockBean
    protected SettingService settingService;

    @Autowired
    protected UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RabbitOperations jobsQueueManager;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private ApplicationEventMulticaster applicationEventMulticaster;

    private final List<ApplicationListener<?>> listenersForTest = new LinkedList<>();

    @Before
    public void mockSettingsService() {
        Settings s = new Settings();
        s.setServerUrl("http://127.0.0.1:8080");

        Mockito.when(settingService.get()).thenReturn(s);
    }

    @After
    public void cleanListeners() {
        for (ApplicationListener<?> listener : listenersForTest) {
            applicationEventMulticaster.removeApplicationListener(listener);
        }
    }

    @After
    public void dbAndQueueCleanUp() {
        for (Flow flow : flowDao.findAll()) {
            jobsQueueManager.delete(flow.getQueueName());
        }

        mongoTemplate.dropCollection(Flow.class);
        mongoTemplate.dropCollection(FlowYml.class);
        mongoTemplate.dropCollection(FlowUsers.class);
        mongoTemplate.dropCollection(Agent.class);
        mongoTemplate.dropCollection(SimpleUser.class);
        mongoTemplate.dropCollection(Config.class);
        mongoTemplate.dropCollection(MatrixItem.class);
        mongoTemplate.dropCollection(GitConfig.class);
        mongoTemplate.dropCollection(Job.class);
        mongoTemplate.dropCollection(JobAgent.class);
        mongoTemplate.dropCollection(JobArtifact.class);
        mongoTemplate.dropCollection(JobReport.class);
        mongoTemplate.dropCollection(JobNumber.class);
        mongoTemplate.dropCollection(JobPriority.class);
        mongoTemplate.dropCollection(JobYml.class);
        mongoTemplate.dropCollection(RelatedJobs.class);
        mongoTemplate.dropCollection(Step.class);
        mongoTemplate.dropCollection(Plugin.class);
        mongoTemplate.dropCollection(Secret.class);
        mongoTemplate.dropCollection(Trigger.class);
        mongoTemplate.dropCollection(TriggerDelivery.class);
        mongoTemplate.dropCollection(User.class);
    }

    protected void addEventListener(ApplicationListener<?> listener) {
        applicationEventMulticaster.addApplicationListener(listener);
        listenersForTest.add(listener);
    }

    protected void removeListener(ApplicationListener<?> listener) {
        applicationEventMulticaster.removeApplicationListener(listener);
        listenersForTest.remove(listener);
    }

    protected void multicastEvent(ApplicationEvent event) {
        applicationEventMulticaster.multicastEvent(event);
    }

    protected InputStream load(String resource) {
        return SpringScenario.class.getClassLoader().getResourceAsStream(resource);
    }
}
