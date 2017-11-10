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
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
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
    }



    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitLabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
