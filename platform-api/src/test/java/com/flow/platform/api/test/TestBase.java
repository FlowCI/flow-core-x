package com.flow.platform.api.test;/*
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.api.config.WebConfig;
import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.dao.JobYmlStorageDao;
import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.test.util.NodeUtilYmlTest;
import com.flow.platform.domain.Cmd;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


/**
 * @author yh@fir.im
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class})
@PropertySource("classpath:app-default.properties")
@PropertySource("classpath:i18n")
public abstract class TestBase {

    static {
        System.setProperty("flow.api.env", "test");
        System.setProperty("flow.api.task.keep_idle_agent", "false");
    }

    @Autowired
    protected FlowDao flowDao;

    @Autowired
    protected JobDao jobDao;

    @Autowired
    protected YmlStorageDao ymlStorageDao;

    @Autowired
    protected JobYmlStorageDao jobYmlStorageDao;

    @Autowired
    protected NodeResultDao nodeResultDao;

    @Autowired
    private WebApplicationContext webAppContext;

    protected MockMvc mockMvc;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    public String getResourceContent(String fileName) throws IOException {
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        File path = new File(resource.getFile());
        return Files.toString(path, Charset.forName("UTF-8"));
    }

    public void stubDemo() {
        Cmd cmdRes = new Cmd();
        cmdRes.setId(UUID.randomUUID().toString());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));
    }

    private void cleanDatabase() {
        flowDao.deleteAll();
        jobDao.deleteAll();
        ymlStorageDao.deleteAll();
        jobYmlStorageDao.deleteAll();
        nodeResultDao.deleteAll();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @After
    public void afterEach() {
        cleanDatabase();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        // clean up cmd log folder
    }
}