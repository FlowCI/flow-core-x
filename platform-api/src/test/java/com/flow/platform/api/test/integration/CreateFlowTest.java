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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.GitService.Env;
import com.flow.platform.api.service.NodeService;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

/**
 * @author yang
 */
public class CreateFlowTest extends TestBase {

    private final static String GIT_URL = "git@github.com:flow-ci-plugin/for-testing.git";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private Path workspace;

    @Test
    public void should_create_flow_and_init_yml() throws Throwable {
        // create empty flow
        Flow flow = nodeService.createEmptyFlow("/flow-integration");
        Assert.assertNotNull(nodeService.find(flow.getPath()));

        // setup git related env
        Map<String, String> env = new HashMap<>();
        env.put(GitService.Env.FLOW_GIT_SOURCE, GitSource.UNDEFINED_SSH.name());
        env.put(GitService.Env.FLOW_GIT_URL, GIT_URL);
        env.put(Env.FLOW_GIT_SSH_PRIVATE_KEY, getResourceContent("ssh_private_key"));
        nodeService.setFlowEnv(flow.getPath(), env);

        final Flow loaded = (Flow) nodeService.find(flow.getPath());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), loaded.getEnvs().get(GitService.Env.FLOW_GIT_SOURCE));
        Assert.assertEquals(GIT_URL, loaded.getEnvs().get(GitService.Env.FLOW_GIT_URL));

        // async to clone and return .flow.yml content
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder yml = new StringBuilder();
        nodeService.loadYmlContent(loaded.getPath(), ymlStorage -> {
            yml.append(ymlStorage.getFile());
            latch.countDown();
        });

        latch.await(60, TimeUnit.SECONDS);

        // create node by yml content
        nodeService.create(loaded.getPath(), yml.toString());

        YmlStorage ymlStorage = ymlStorageDao.get(loaded.getPath());
        Assert.assertNotNull(ymlStorage);
        Assert.assertEquals(yml.toString(), ymlStorage.getFile());
    }

    @After
    public void after() {
        FileSystemUtils.deleteRecursively(Paths.get(workspace.toString(), "flow-integration").toFile());
    }
}
