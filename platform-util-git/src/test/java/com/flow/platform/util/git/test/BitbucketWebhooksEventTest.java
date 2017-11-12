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

package com.flow.platform.util.git.test;

import com.flow.platform.util.git.hooks.BitbucketEvents.Hooks;
import com.flow.platform.util.git.hooks.GitHookEventFactory;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class BitbucketWebhooksEventTest {


    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("bitbucket/webhook_push.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.BITBUCKET, event.getGitSource());
        Assert.assertEquals(GitEventType.PUSH, event.getType());
        Assert.assertEquals("200b5197debd10e0ca341e640422368b145eb254", event.getAfter());
        Assert.assertEquals("9429d1ee9fa16dc53d4dc5a0937693330aeda8bb", event.getBefore());
        Assert.assertEquals(
            "https://bitbucket.org/CodingWill/info/branches/compare/200b5197debd10e0ca341e640422368b145eb254..9429d1ee9fa16dc53d4dc5a0937693330aeda8bb",
            event.getCompareUrl());
        Assert.assertEquals("https://bitbucket.org/CodingWill/info/commits/200b5197debd10e0ca341e640422368b145eb254",
            event.getHeadCommitUrl());
        Assert.assertEquals("add 5\n", event.getMessage());
        Assert.assertEquals("CodingWill", event.getUsername());
        Assert.assertEquals("CodingWill", event.getUserEmail());
        Assert.assertEquals(4, event.getCommits().size());
    }

    @Test
    public void should_convert_to_tag_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("bitbucket/webhook_tag.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.BITBUCKET, event.getGitSource());
        Assert.assertEquals(GitEventType.TAG, event.getType());

        Assert.assertEquals("CodingWill", event.getUserEmail());
        Assert.assertEquals("CodingWill", event.getUsername());
        Assert.assertEquals("add tag\n", event.getMessage());
        Assert.assertEquals("https://bitbucket.org/CodingWill/info/commits/86edd5e14fd18bcf923b8eca522319896b2090e4",
            event.getHeadCommitUrl());
        Assert.assertEquals("86edd5e14fd18bcf923b8eca522319896b2090e4", event.getAfter());
        Assert.assertEquals(0, event.getCommits().size());
    }

    @Test
    public void should_convert_to_pr_open_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("bitbucket/webhook_pr_open.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_PR_CREATED);
        GitPullRequestEvent event = (GitPullRequestEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.BITBUCKET, event.getGitSource());
        Assert.assertEquals(GitEventType.PR, event.getType());

        Assert.assertEquals("", event.getDescription());
        Assert.assertEquals("https://bitbucket.org/CodingWill/info/pull-requests/6", event.getUrl());
        Assert.assertEquals("CodingWill", event.getSubmitter());
        Assert.assertNull(event.getMergedBy());

        GitPullRequestInfo source = event.getSource();
        Assert.assertEquals("feature/1", source.getBranch());
        Assert.assertEquals("ac66189104f3", source.getSha());
        Assert.assertEquals("CodingWill/info", source.getProjectName());

        GitPullRequestInfo target = event.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals("eb612981e942", target.getSha());
        Assert.assertEquals("CodingWill/info", target.getProjectName());
    }

    @Test
    public void should_convert_to_pr_close_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("bitbucket/webhook_pr_close.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_PR_MERGERED);
        GitPullRequestEvent event = (GitPullRequestEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.BITBUCKET, event.getGitSource());
        Assert.assertEquals(GitEventType.PR, event.getType());

        Assert.assertEquals("https://bitbucket.org/CodingWill/info/pull-requests/3", event.getUrl());
        Assert.assertEquals("CodingWill", event.getSubmitter());
        Assert.assertNotNull(event.getMergedBy());

        GitPullRequestInfo source = event.getSource();
        Assert.assertEquals("develop", source.getBranch());
        Assert.assertEquals("61d01698184b", source.getSha());
        Assert.assertEquals("CodingWill/info", source.getProjectName());

        GitPullRequestInfo target = event.getTarget();
        Assert.assertEquals("feature/test", target.getBranch());
        Assert.assertEquals("c7d2fca28131", target.getSha());
        Assert.assertEquals("CodingWill/info", target.getProjectName());
    }


    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitLabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
