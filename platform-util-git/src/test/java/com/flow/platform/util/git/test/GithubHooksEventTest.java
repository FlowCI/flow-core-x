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
import com.flow.platform.util.git.hooks.GithubEvents.Hooks;
import com.flow.platform.util.git.model.GitEventCommit;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class GithubHooksEventTest {

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("github/webhook_push.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);

        // when:
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(dummyHeader, pushEventContent);
        Assert.assertNotNull(event);

        // then: verify event
        Assert.assertEquals("refs/heads/master", event.getRef());
        Assert.assertEquals("5a1e8ee1007b742fba00da1f66b0cc5e3bc5f024", event.getBefore());
        Assert.assertEquals("40d0dd6e8e942643d794d7ed8d27610fb8729914", event.getAfter());
        Assert.assertEquals("23307997", event.getUserId());
        Assert.assertEquals("yang-guo-2016", event.getUsername());

        // then: verify commit
        List<GitEventCommit> commits = event.getCommits();
        Assert.assertNotNull(commits);
        Assert.assertEquals(1, commits.size());

        GitEventCommit commit = commits.get(0);
        Assert.assertEquals("40d0dd6e8e942643d794d7ed8d27610fb8729914", commit.getId());
        Assert.assertEquals("fdafadsf\n\ndfsdafad", commit.getMessage());
        Assert.assertEquals("2017-08-08T11:19:05+08:00", commit.getTimestamp());
        Assert.assertEquals("gradlew", commit.getModified().get(0));

        // then: verify commit author
        Assert.assertEquals("yang.guo", commit.getAuthor().getName());
        Assert.assertEquals("gy@fir.im", commit.getAuthor().getEmail());
    }

    @Test
    public void should_convert_to_tag_event_obj() throws Throwable {
        // given:
        String tagEventContent = loadWebhookSampleJson("github/webhook_tag.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);

        // when:
        GitPushTagEvent event = (GitPushTagEvent) GitHookEventFactory.build(dummyHeader, tagEventContent);
        Assert.assertNotNull(event);

        // then:
        Assert.assertEquals(GitSource.GITHUB, event.getGitSource());
        Assert.assertEquals(GitEventType.TAG, event.getType());

        Assert.assertEquals("refs/tags/v1.6", event.getRef());
        Assert.assertEquals("refs/heads/developer", event.getBaseRef());
        Assert.assertEquals("0000000000000000000000000000000000000000", event.getBefore());
        Assert.assertEquals("26d1d0fa6ee44a8f4e02250d13e84bf02722f5e7", event.getAfter());
        Assert.assertEquals("23307997", event.getUserId());
        Assert.assertEquals("yang-guo-2016", event.getUsername());
        Assert.assertEquals(0, event.getCommits().size());
    }

    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitlabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
