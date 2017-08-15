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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.context.SpringContext;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.git.GitWebhookTriggerFinishEvent;
import com.flow.platform.api.service.GitService.Env;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.util.ObjectWrapper;
import com.flow.platform.util.git.model.GitSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class GitWebhookTest extends TestBase {

    private final String flowName = "flow-integration";

    private final String flowPath = PathUtil.build(flowName);

    private final String gitUrl = "git@github.com:flow-ci-plugin/for-testing.git";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private SpringContext springContext;

    @Test
    public void should_create_job_after_git_webhook_trigger() throws Throwable {
        stubDemo();

        init_flow();

        push_trigger_from_git();
    }

    private void init_flow() throws Throwable {
        // create empty flow
        nodeService.createEmptyFlow(flowName);

        // set flow git related env
        Map<String, String> env = new HashMap<>();
        env.put(Env.FLOW_GIT_SOURCE, GitSource.UNDEFINED_SSH.name());
        env.put(Env.FLOW_GIT_URL, gitUrl);
        env.put(Env.FLOW_GIT_BRANCH, "develop");
        env.put(Env.FLOW_GIT_SSH_PRIVATE_KEY, getResourceContent("ssh_private_key"));
        nodeService.setFlowEnv(flowPath, env);

        Node loaded = nodeService.find(flowPath);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(4, loaded.getEnvs().size());
        Assert.assertNull(nodeService.getYmlContent(flowPath));
    }

    private void push_trigger_from_git() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ObjectWrapper<Job> wrapper = new ObjectWrapper<>();

        final ApplicationListener<GitWebhookTriggerFinishEvent> listener = event -> {
            latch.countDown();
            wrapper.setInstance((Job) event.getSource());
        };

        springContext.registerApplicationListener(listener);

        MockHttpServletRequestBuilder push = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("github/push_webhook_payload"))
            .header("x-github-event", "push")
            .header("x-github-delivery", "29087180-8177-11e7-83a4-3b68852f0c9e");

        mockMvc.perform(push).andExpect(status().isOk());

        // wait for 60 seconds for git pull
        latch.await(60, TimeUnit.SECONDS);
        springContext.removeApplicationListener(listener);

        // verify yml is updated
        Assert.assertNotNull(nodeService.getYmlContent(flowPath));

        // verify job is created
        Job created = wrapper.getInstance();
        Assert.assertEquals(flowPath, created.getNodePath());
        Assert.assertEquals(1, created.getNumber().intValue());
        Assert.assertNotNull(created.getCmdId());
    }
}
