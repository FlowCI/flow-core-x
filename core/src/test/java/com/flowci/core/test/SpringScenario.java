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

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.common.service.SettingService;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.test.SpringScenario.Config;
import com.flowci.core.test.auth.AuthHelper;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.Assert;
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
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
@Log4j2
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
    }

    @MockBean
    protected SettingService settingService;

    @Autowired
    protected SessionManager sessionManager;

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
    public void dbCleanUp() {
        mongoTemplate.getDb().drop();
    }

    @After
    public void cleanListeners() {
        for (ApplicationListener<?> listener : listenersForTest) {
            applicationEventMulticaster.removeApplicationListener(listener);
        }
    }

    @After
    public void queueCleanUp() {
        for (Flow flow : flowDao.findAll()) {
            jobsQueueManager.delete(flow.getQueueName());
        }
    }

    protected void should_has_db_info(Mongoable obj) {
        Assert.assertNotNull(obj.getCreatedAt());
        Assert.assertNotNull(obj.getUpdatedAt());
        Assert.assertEquals(sessionManager.getUserEmail(), obj.getCreatedBy());
        Assert.assertEquals(sessionManager.getUserEmail(), obj.getUpdatedBy());
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
