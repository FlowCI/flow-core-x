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

import com.flow.platform.util.git.hooks.GitHookEventFactory;
import com.flow.platform.util.git.hooks.OschinaEvents;
import com.flow.platform.util.git.hooks.OschinaEvents.Hooks;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent.State;
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
public class OschinaEventTest {

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("oschina/webhook_push.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.OSCHINA, event.getGitSource());
        Assert.assertEquals(GitEventType.PUSH, event.getType());
        Assert.assertEquals("be9cd46d9dd4067846a7a486c599bd44f29777e2", event.getAfter());
        Assert.assertEquals("039ace200fe4e142d89e6dd4f3531a7f3765b4b0", event.getBefore());
        Assert.assertEquals(
            "https://gitee.com/willcoding/fir_cli/compare/039ace200fe4...be9cd46d9dd4",
            event.getCompareUrl());
        Assert.assertEquals("https://gitee.com/willcoding/fir_cli/commit/be9cd46d9dd4067846a7a486c599bd44f29777e2",
            event.getHeadCommitUrl());
        Assert.assertEquals("change", event.getMessage());
        Assert.assertEquals("CodingWill", event.getUsername());
        Assert.assertEquals("yh@fir.im", event.getUserEmail());
        Assert.assertEquals(1, event.getCommits().size());
    }


    @Test
    public void should_convert_to_tag_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("oschina/webhook_tag.json");
        Map<String, String> header = new HashMap<>();
        header.put(Hooks.HEADER, Hooks.EVENT_TYPE_TAG);
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.OSCHINA, event.getGitSource());
        Assert.assertEquals(GitEventType.TAG, event.getType());
        Assert.assertEquals("d2c58d16816f9722b48d9d00a0180c3c4da0486a", event.getAfter());
        Assert.assertEquals("0000000000000000000000000000000000000000", event.getBefore());
        Assert.assertEquals(
            "https://gitee.com/willcoding/fir_cli/compare/d2c58d16816f...2.0",
            event.getCompareUrl());
        Assert.assertEquals("https://gitee.com/willcoding/fir_cli/commit/d2c58d16816f9722b48d9d00a0180c3c4da0486a",
            event.getHeadCommitUrl());
        Assert.assertEquals(null, event.getMessage());
        Assert.assertEquals("CodingWill", event.getUsername());
        Assert.assertEquals("CodingWill", event.getUserEmail());
    }

    @Test
    public void should_convert_to_pr_open_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("oschina/webhook_pr_open.json");
        Map<String, String> header = new HashMap<>();
        header.put(OschinaEvents.Hooks.HEADER, Hooks.EVENT_TYPE_PR);
        GitPullRequestEvent event = (GitPullRequestEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(State.OPEN, event.getState());
        Assert.assertEquals(GitSource.OSCHINA, event.getGitSource());
        Assert.assertEquals(GitEventType.PR, event.getType());

        Assert.assertEquals("make pr", event.getDescription());
        Assert.assertEquals("https://gitee.com/willcoding/fir_cli/pulls/1", event.getUrl());
        Assert.assertEquals("yh@fir.im", event.getSubmitter());
        Assert.assertNull(event.getMergedBy());

        GitPullRequestInfo source = event.getSource();
        Assert.assertEquals("feature/develop", source.getBranch());
        Assert.assertEquals("CodingWill/fir_cli", source.getProjectName());

        GitPullRequestInfo target = event.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals("CodingWill/fir_cli", target.getProjectName());
    }

    @Test
    public void should_convert_to_pr_close_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("oschina/webhook_pr_close.json");
        Map<String, String> header = new HashMap<>();
        header.put(OschinaEvents.Hooks.HEADER, Hooks.EVENT_TYPE_PR);
        GitPullRequestEvent event = (GitPullRequestEvent) GitHookEventFactory.build(header, pushEventContent);
        Assert.assertNotNull(event);
        Assert.assertEquals(GitSource.OSCHINA, event.getGitSource());
        Assert.assertEquals(GitEventType.PR, event.getType());
        Assert.assertEquals(State.CLOSE, event.getState());

        Assert.assertEquals("make pr", event.getDescription());
        Assert.assertEquals("https://gitee.com/willcoding/fir_cli/pulls/2", event.getUrl());
        Assert.assertEquals("CodingWill", event.getMergedBy());

        GitPullRequestInfo source = event.getSource();
        Assert.assertEquals("feature/develop", source.getBranch());
        Assert.assertEquals("CodingWill/fir_cli", source.getProjectName());

        GitPullRequestInfo target = event.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals("CodingWill/fir_cli", target.getProjectName());
    }

    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitLabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
