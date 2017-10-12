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

import com.flow.platform.api.domain.envs.EnvKey;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.git.GitWebhookTriggerFinishEvent;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.util.ObjectUtil;
import com.flow.platform.util.ObjectWrapper;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class GitWebhookTest extends TestBase {

    private final String flowName = "flow_integration";

    private final String flowPath = PathUtil.build(flowName);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private SpringContext springContext;

    @Before
    public void before() throws IOException {
        stubDemo();
    }

    @Test
    public void should_create_job_after_github_push_webhook_trigger() throws Throwable {
        init_flow(GITHUB_TEST_REPO_SSH);

        MockHttpServletRequestBuilder push = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("github/push_payload.json"))
            .header("x-github-event", "push")
            .header("x-github-delivery", "29087180-8177-11e7-83a4-3b68852f0c9e");

        Job job = mock_trigger_from_git(push);
        job = jobDao.get(job.getId());

        Set<String> envKeySet = Sets.newHashSet(ObjectUtil.deepCopy(EnvKey.FOR_OUTPUTS));
        envKeySet.remove(GitEnvs.FLOW_GIT_PR_URL.name());
        verifyRootNodeResultOutput(job, envKeySet);

        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PUSH.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITHUB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("master", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("Update .flow.yml for github", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
        Assert.assertEquals("yang-guo-2016", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("gy@fir.im", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR_EMAIL));
        Assert.assertEquals("daedd0ff0feca54f4642a872081418d1510b4368", job.getEnv(GitEnvs.FLOW_GIT_COMMIT_ID));
        Assert.assertEquals(
            "https://github.com/flow-ci-plugin/for-testing/commit/daedd0ff0feca54f4642a872081418d1510b4368",
            job.getEnv(GitEnvs.FLOW_GIT_COMMIT_URL));
        Assert.assertEquals("9436bd5e0c06...daedd0ff0fec", job.getEnv(GitEnvs.FLOW_GIT_COMPARE_ID));
        Assert.assertEquals("https://github.com/flow-ci-plugin/for-testing/compare/9436bd5e0c06...daedd0ff0fec",
            job.getEnv(GitEnvs.FLOW_GIT_COMPARE_URL));
    }

    @Test
    public void should_create_job_after_github_open_pr_webhook_trigger() throws Throwable {
        init_flow(GITHUB_TEST_REPO_SSH);

        MockHttpServletRequestBuilder openPr = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("github/pr_open_payload.json"))
            .header("x-github-event", "pull_request")
            .header("x-github-delivery", "29087180-8177-11e7-83a4-3b68852f0c9e");

        Job job = mock_trigger_from_git(openPr);
        job = jobDao.get(job.getId());

        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PR.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITHUB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("develop", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("https://github.com/flow-ci-plugin/for-testing/pull/2",
            job.getEnv(GitEnvs.FLOW_GIT_PR_URL));
        Assert.assertEquals("yang-guo-2016", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("Update README.md", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
    }

    @Test
    public void should_create_job_after_github_close_pr_webhook_trigger() throws Throwable {
        init_flow(GITHUB_TEST_REPO_SSH);

        MockHttpServletRequestBuilder createPr = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("github/pr_close_payload.json"))
            .header("x-github-event", "pull_request")
            .header("x-github-delivery", "29087180-8177-11e7-83a4-3b68852f0c9e");

        Job job = mock_trigger_from_git(createPr);
        job = jobDao.get(job.getId());

        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PR.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITHUB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("master", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("https://github.com/flow-ci-plugin/for-testing/pull/1",
            job.getEnv(GitEnvs.FLOW_GIT_PR_URL));
        Assert.assertEquals("yang-guo-2016", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("Develop", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
    }

    @Test
    public void should_create_job_after_gitlab_push_webhook_trigger() throws Throwable {
        init_flow("git@gitlab.com:yang.guo/for-testing.git");

        MockHttpServletRequestBuilder push = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("gitlab/push_payload.json"))
            .header("x-gitlab-event", "Push Hook");

        Job job = mock_trigger_from_git(push);
        job = jobDao.get(job.getId());

        Set<String> envKeySet = Sets.newHashSet(ObjectUtil.deepCopy(EnvKey.FOR_OUTPUTS));
        envKeySet.remove(GitEnvs.FLOW_GIT_PR_URL.name());
        verifyRootNodeResultOutput(job, envKeySet);

        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PUSH.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITLAB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("Update .flow.yml for gitlab", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG.name()));
        Assert.assertEquals("develop", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("yang.guo", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("benqyang_2006@hotmail.com", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR_EMAIL));
        Assert.assertEquals("2d9b3a080c8fb653686cd56ccf8c0a6b50ba47d3", job.getEnv(GitEnvs.FLOW_GIT_COMMIT_ID));
        Assert.assertEquals(
            "https://gitlab.com/yang.guo/for-testing/commit/2d9b3a080c8fb653686cd56ccf8c0a6b50ba47d3",
            job.getEnv(GitEnvs.FLOW_GIT_COMMIT_URL));
        Assert.assertEquals("c9ca9280a567...2d9b3a080c8f", job.getEnv(GitEnvs.FLOW_GIT_COMPARE_ID));
        Assert.assertEquals(
            "https://gitlab.com/yang.guo/for-testing/compare/c9ca9280a567...2d9b3a080c8f",
            job.getEnv(GitEnvs.FLOW_GIT_COMPARE_URL));
    }

    @Test
    public void should_create_job_after_gitlab_open_pr_webhook_trigger() throws Throwable {
        init_flow("git@gitlab.com:yang.guo/for-testing.git");

        // when: mock pr request from gitlab
        MockHttpServletRequestBuilder openPr = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("gitlab/pr_open_payload.json"))
            .header("x-gitlab-event", "Merge Request Hook");

        // then: should should be created
        Job job = mock_trigger_from_git(openPr);
        job = jobDao.get(job.getId());

        // then: verify job env
        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PR.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITLAB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("develop", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("https://gitlab.com/yang.guo/for-testing/merge_requests/1",
            job.getEnv(GitEnvs.FLOW_GIT_PR_URL));
        Assert.assertEquals("yang.guo", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("Develop", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
    }

    @Test
    public void should_create_job_after_gitlab_close_pr_webhook_trigger() throws Throwable {
        init_flow("git@gitlab.com:yang.guo/for-testing.git");

        // when: mock pr request from gitlab
        MockHttpServletRequestBuilder openPr = post("/hooks/git/" + flowName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceContent("gitlab/pr_close_payload.json"))
            .header("x-gitlab-event", "Merge Request Hook");

        // then: should should be created
        Job job = mock_trigger_from_git(openPr);
        job = jobDao.get(job.getId());

        // then: verify job env
        Assert.assertEquals(GitSource.UNDEFINED_SSH.name(), job.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Assert.assertEquals(GitEventType.PR.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_TYPE));
        Assert.assertEquals(GitSource.GITLAB.name(), job.getEnv(GitEnvs.FLOW_GIT_EVENT_SOURCE));

        Assert.assertEquals("master", job.getEnv(GitEnvs.FLOW_GIT_BRANCH));
        Assert.assertEquals("https://gitlab.com/yang.guo/for-testing/merge_requests/2",
            job.getEnv(GitEnvs.FLOW_GIT_PR_URL));
        Assert.assertEquals("yang.guo", job.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertEquals("Update README.md 1123", job.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
    }

    private void init_flow(String gitUrl) throws Throwable {
        // create empty flow
        Flow flow = nodeService.createEmptyFlow(flowName);
        setFlowToReady(flow);

        // set flow git related env
        Map<String, String> env = new HashMap<>();
        env.put(GitEnvs.FLOW_GIT_SOURCE.name(), GitSource.UNDEFINED_SSH.name());
        env.put(GitEnvs.FLOW_GIT_URL.name(), gitUrl);
        env.put(GitEnvs.FLOW_GIT_BRANCH.name(), "develop");
        env.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), getResourceContent("ssh_private_key"));
        nodeService.addFlowEnv(flow, env);

        Node loaded = nodeService.find(flowPath);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(7, loaded.getEnvs().size());
        Assert.assertEquals(FlowEnvs.YmlStatusValue.NOT_FOUND.value(), loaded.getEnv(FlowEnvs.FLOW_YML_STATUS));
    }

    private Job mock_trigger_from_git(RequestBuilder push) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ObjectWrapper<Job> wrapper = new ObjectWrapper<>();

        final ApplicationListener<GitWebhookTriggerFinishEvent> listener = event -> {
            latch.countDown();
            wrapper.setInstance((Job) event.getSource());
        };

        springContext.registerApplicationListener(listener);

        mockMvc.perform(push).andExpect(status().isOk());

        // wait for 60 seconds for git pull
        latch.await(60, TimeUnit.SECONDS);
        springContext.removeApplicationListener(listener);

        // verify yml is updated
        Node root = nodeService.find(flowPath);
        Assert.assertNotNull(ymlService.getYmlContent(root));

        // verify job is created
        Job created = wrapper.getInstance();
        Assert.assertEquals(flowPath, created.getNodePath());
        Assert.assertEquals(1, created.getNumber().intValue());

        // verify flow node yml status
        Node flowNode = nodeService.find(created.getNodePath());
        Assert.assertEquals(YmlStatusValue.FOUND.value(), flowNode.getEnv(FlowEnvs.FLOW_YML_STATUS));

        return created;
    }

    private void verifyRootNodeResultOutput(Job job, Set<String> requiredKeys) {
        // verify env which needs write to output of root node result
        for (String outputKey : requiredKeys) {
            String envValue = job.getRootResult().getOutputs().get(outputKey);
            Assert.assertTrue(envValue != null);
        }
    }
}
