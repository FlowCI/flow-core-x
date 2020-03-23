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

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.test.SpringScenario.Config;
import com.flowci.core.test.auth.AuthHelper;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.domain.Agent;
import com.flowci.exception.NotFoundException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
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
        public Class<?> httpJacksonMixin() {
            return VarsMixinIgnore.class;
        }

        public interface VarsMixinIgnore {

        }
    }

    @Autowired
    protected SessionManager sessionManager;

    @Autowired
    protected UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RabbitQueueOperation callbackQueueManager;

    @Autowired
    private RabbitQueueOperation loggingQueueManager;

    @Autowired
    private RabbitChannelOperation agentQueueManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private ApplicationEventMulticaster applicationEventMulticaster;

    private final List<ApplicationListener<?>> listenersForTest = new LinkedList<>();

    @After
    public void cleanListeners() {
        for (ApplicationListener listener : listenersForTest) {
            applicationEventMulticaster.removeApplicationListener(listener);
        }
    }

    @After
    public void dbCleanUp() {
        mongoTemplate.getDb().drop();
    }

    @After
    public void queueCleanUp() {
        callbackQueueManager.purge();
        loggingQueueManager.purge();

        for (Agent agent : agentDao.findAll()) {
            agentQueueManager.delete(agent.getQueueName());
        }

        for (Flow flow : flowDao.findAll()) {
            flowJobQueueManager.remove(flow.getQueueName());
        }
    }

    protected void addEventListener(ApplicationListener<?> listener) {
        applicationEventMulticaster.addApplicationListener(listener);
        listenersForTest.add(listener);
    }

    protected void multicastEvent(ApplicationEvent event) {
        applicationEventMulticaster.multicastEvent(event);
    }

    protected InputStream load(String resource) {
        return SpringScenario.class.getClassLoader().getResourceAsStream(resource);
    }

    protected void mockLogin() {
        User user;
        try {
            user = userService.getByEmail("test@flow.ci");
        } catch (NotFoundException e) {
            user = userService.create("test@flow.ci", "12345", User.Role.Admin);
        }
        sessionManager.set(user);
    }
}
