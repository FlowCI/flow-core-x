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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.config.WebConfig;
import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.MessageSettingDao;
import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.dao.job.JobYmlDao;
import com.flow.platform.api.dao.job.NodeResultDao;
import com.flow.platform.api.dao.user.ActionDao;
import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserFlowDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.FlowEnvs.StatusValue;
import com.flow.platform.api.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.initializers.Initializer;
import com.flow.platform.api.initializers.UserRoleInit;
import com.flow.platform.api.service.job.CmdService;
import com.flow.platform.api.service.job.JobSearchService;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.job.NodeResultService;
import com.flow.platform.api.service.node.EnvService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.git.model.GitSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author yh@fir.im
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class})
@PropertySource("classpath:app-default.properties")
@PropertySource("classpath:i18n")
@FixMethodOrder(MethodSorters.JVM)
public abstract class TestBase {

    protected final static String GITHUB_TEST_REPO_SSH = "git@github.com:flow-ci-plugin/for-testing.git";
    protected final static String GITHUB_TEST_REPO_HTTP = "https://github.com/flow-ci-plugin/for-testing.git";

    static {
        System.setProperty("flow.api.env", "test");
        System.setProperty("flow.api.task.keep_idle_agent", "false");
    }

    @Autowired
    protected FlowDao flowDao;

    @Autowired
    protected JobDao jobDao;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected YmlDao ymlDao;

    @Autowired
    protected JobYmlDao jobYmlDao;

    @Autowired
    protected NodeResultDao nodeResultDao;

    @Autowired
    protected CredentialDao credentialDao;

    @Autowired
    protected MessageSettingDao messageSettingDao;

    @Autowired
    protected NodeService nodeService;

    @Autowired
    protected EnvService envService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected NodeResultService nodeResultService;

    @Autowired
    protected JobSearchService searchService;

    @Autowired
    protected WebApplicationContext webAppContext;

    @Autowired
    protected RoleDao roleDao;

    @Autowired
    protected ActionDao actionDao;

    @Autowired
    protected UserRoleDao userRoleDao;

    @Autowired
    protected PermissionDao permissionDao;

    @Autowired
    protected Path workspace;

    @Autowired
    protected UserFlowDao userFlowDao;

    @Autowired
    protected ThreadLocal<User> currentUser;

    @Value(value = "${system.email}")
    private String email;

    @Value(value = "${system.username}")
    private String username;

    @Value(value = "${system.password}")
    private String password;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    protected MockMvc mockMvc;

    protected User mockUser = new User("test@flow.ci", "ut", "");

    private static Path WORKSPACE;

    static {
        Initializer.ENABLED = false;
    }

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        WORKSPACE = workspace;
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
        setCurrentUser(null);
    }

    public void setCurrentUser(User user) {
        if (user == null) {
            user = userDao.get(email);
            if (user == null) {
                User testUser = new User(email, username, password);
                userDao.save(testUser);
                currentUser.set(testUser);
            } else {
                currentUser.set(user);
            }

            return;
        }

        currentUser.set(user);
    }

    public String getResourceContent(String fileName) throws IOException {
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        File path = new File(resource.getFile());
        return Files.toString(path, AppConfig.DEFAULT_CHARSET);
    }

    public Node createRootFlow(String flowName, String ymlResourceName) throws IOException {
        Node emptyFlow = nodeService.createEmptyFlow(flowName);
        setFlowToReady(emptyFlow);
        String yml = getResourceContent(ymlResourceName);
        setRequiredJobEnvsForFlow(emptyFlow);
        return nodeService.createOrUpdateYml(emptyFlow.getPath(), yml);
    }

    public void setFlowToReady(Node flowNode) {
        Map<String, String> envs = new HashMap<>();
        envs.put(FlowEnvs.FLOW_STATUS.name(), FlowEnvs.StatusValue.READY.value());
        envService.save(flowNode, envs, false);
    }

    public void setRequiredJobEnvsForFlow(Node flow) throws IOException {
        HashMap<String, String> envs = new HashMap<>();
        envs.put(FlowEnvs.FLOW_STATUS.name(), StatusValue.READY.value());
        envs.put(FlowEnvs.FLOW_YML_STATUS.name(), YmlStatusValue.FOUND.value());
        envs.put(GitEnvs.FLOW_GIT_URL.name(), TestBase.GITHUB_TEST_REPO_SSH);
        envs.put(GitEnvs.FLOW_GIT_SOURCE.name(), GitSource.UNDEFINED_SSH.name());
        envs.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), getResourceContent("ssh_private_key"));
        envService.save(flow, envs, false);
    }

    public void stubDemo() {
        Cmd mockCmdResponse = new Cmd();
        mockCmdResponse.setId(UUID.randomUUID().toString());
        mockCmdResponse.setSessionId(UUID.randomUUID().toString());

        wireMockRule.resetAll();

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(mockCmdResponse.toJson())));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse()
                .withBody(mockCmdResponse.toJson())));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/stop/" + mockCmdResponse.getId()))
            .willReturn(aResponse()
                .withBody(mockCmdResponse.toJson())));

        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("step_log.zip");
        File path = new File(resource.getFile());

        try (InputStream inputStream = new FileInputStream(path)) {
            stubFor(com.github.tomakehurst.wiremock.client.WireMock
                .get(urlPathEqualTo("/cmd/log/download"))
                .willReturn(aResponse().withBody(org.apache.commons.io.IOUtils.toByteArray(inputStream))));
        } catch (Throwable throwable) {
        }
    }

    public String performRequestWith200Status(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString();
    }

    private void cleanDatabase() {
        flowDao.deleteAll();
        jobDao.deleteAll();
        ymlDao.deleteAll();
        jobYmlDao.deleteAll();
        nodeResultDao.deleteAll();
        userDao.deleteAll();
        credentialDao.deleteAll();
        messageSettingDao.deleteAll();
        roleDao.deleteAll();
        actionDao.deleteAll();
        userRoleDao.deleteAll();
        permissionDao.deleteAll();
        userFlowDao.deleteAll();
    }

    @After
    public void afterEach() {
        cleanDatabase();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        FileSystemUtils.deleteRecursively(WORKSPACE.toFile());
    }

}