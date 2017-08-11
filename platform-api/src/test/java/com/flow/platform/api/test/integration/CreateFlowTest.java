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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitSource;
import java.util.HashMap;
import java.util.Map;
import joptsimple.internal.Strings;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class CreateFlowTest extends TestBase {

    private final static String GIT_URL = "git@github.com:flow-ci-plugin/for-testing.git";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private GitService gitService;

    @Test
    public void should_create_flow_and_init_yml() throws Throwable {
        // create empty flow
        Flow flow = nodeService.createEmptyFlow("/flow-integration");
        Assert.assertNotNull(nodeService.find(flow.getPath()));

        // setup git related env
        Map<String, String> env = new HashMap<>();
        env.put(GitService.ENV_FLOW_GIT_SOURCE, GitSource.UNDEFINED.name());
        env.put(GitService.ENV_FLOW_GIT_URL, GIT_URL);
        nodeService.setFlowEnv(flow.getPath(), env);

        flow = (Flow) nodeService.find(flow.getPath());
        Assert.assertNotNull(flow);
        Assert.assertEquals(GitSource.UNDEFINED.name(), flow.getEnvs().get(GitService.ENV_FLOW_GIT_SOURCE));
        Assert.assertEquals(GIT_URL, flow.getEnvs().get(GitService.ENV_FLOW_GIT_URL));

        // git clone and load .flow.yml file
        String ymlContent = gitService.fetch(flow, AppConfig.DEFAULT_YML_FILE);
        Assert.assertTrue(!Strings.isNullOrEmpty(ymlContent));

        // setup node from yml
        Node root = nodeService.create(ymlContent);
        Assert.assertEquals(flow, root);
        Assert.assertEquals(ymlContent, nodeService.rawYml(flow.getPath()));
    }
}
