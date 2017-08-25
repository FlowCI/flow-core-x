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

package com.flow.platform.api.test.integration;

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.YmlStorage;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileSystemUtils;

/**
 * @author yang
 */
public class CreateFlowTest extends TestBase {

    private final static String GIT_URL = "git@github.com:flow-ci-plugin/for-testing.git";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private Path workspace;

    @Value(value = "${domain}")
    private String domain;

    private Flow flow;

    @Before
    public void before() {
        flow = nodeService.createEmptyFlow("/flow-integration");
        Assert.assertNotNull(nodeService.find(flow.getPath()));
    }

    @Test
    public void should_create_flow_and_init_yml() throws Throwable {
        // setup git related env
        Map<String, String> env = new HashMap<>();
        env.put(GitEnvs.FLOW_GIT_SOURCE.name(), GitSource.UNDEFINED_SSH.name());
        env.put(GitEnvs.FLOW_GIT_URL.name(), GIT_URL);
        env.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), getResourceContent("ssh_private_key"));
        nodeService.setFlowEnv(flow.getPath(), env);

        Flow loaded = (Flow) nodeService.find(flow.getPath());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), loaded.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GIT_URL, loaded.getEnv(GitEnvs.FLOW_GIT_URL));
        Assert.assertEquals(FlowEnvs.YmlStatusValue.NOT_FOUND.value(), loaded.getEnv(FlowEnvs.FLOW_YML_STATUS));

        // async to clone and return .flow.yml content
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] ymlWrapper = {null};

        ymlService.loadYmlContent(loaded.getPath(), ymlStorage -> {
            ymlWrapper[0] = ymlStorage.getFile();
            latch.countDown();
        });

        latch.await(60, TimeUnit.SECONDS);

        loaded = (Flow) nodeService.find(flow.getPath());
        Assert.assertEquals(FlowEnvs.YmlStatusValue.FOUND.value(), loaded.getEnv(FlowEnvs.FLOW_YML_STATUS));

        YmlStorage ymlStorage = ymlStorageDao.get(loaded.getPath());
        Assert.assertNotNull(ymlStorage);
        Assert.assertEquals(ymlWrapper[0], ymlStorage.getFile());
    }

    @Test
    public void should_has_yml_error_message_when_ssh_key_invalid() throws Throwable {
        // setup git related env
        Map<String, String> env = new HashMap<>();
        env.put(GitEnvs.FLOW_GIT_SOURCE.name(), GitSource.UNDEFINED_SSH.name());
        env.put(GitEnvs.FLOW_GIT_URL.name(), GIT_URL);
        env.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), "invalid ssh key xxxx");
        nodeService.setFlowEnv(flow.getPath(), env);

        // async to clone and return .flow.yml content
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] ymlWrapper = {null};

        ymlService.loadYmlContent(flow.getPath(), ymlStorage -> {
            ymlWrapper[0] = ymlStorage.getFile();
            latch.countDown();
        });

        latch.await(60, TimeUnit.SECONDS);

        Node loadedFlow = nodeService.find(flow.getPath());
        Assert.assertEquals(YmlStatusValue.ERROR.value(), loadedFlow.getEnv(FlowEnvs.FLOW_YML_STATUS));
        Assert.assertNotNull(loadedFlow.getEnv(FlowEnvs.FLOW_YML_ERROR_MSG));
    }

    @After
    public void after() {
        FileSystemUtils.deleteRecursively(Paths.get(workspace.toString(), "flow-integration").toFile());
    }
}
