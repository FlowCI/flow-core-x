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

import com.flow.platform.util.git.hooks.CodingEvents.Hooks;
import com.flow.platform.util.git.hooks.CodingEvents.PullRequestAdapter;
import com.flow.platform.util.git.hooks.GitHookEventFactory;
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
 * @author yang
 */
public class CodingWebHooksEventTest {

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("coding/webhook_push.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH_OR_TAG);

        // when: build event from header and json content
        GitPushTagEvent pushEvent = (GitPushTagEvent) GitHookEventFactory.build(dummyHeader, pushEventContent);
        Assert.assertNotNull(pushEvent);

        // then:
        Assert.assertEquals(GitEventType.PUSH, pushEvent.getType());
        Assert.assertEquals(GitSource.CODING, pushEvent.getGitSource());

        Assert.assertEquals("refs/heads/master", pushEvent.getRef());
        Assert.assertEquals("4841b089ecf8dd3dd0010f61bd649bfcc21c1fe8", pushEvent.getBefore());
        Assert.assertEquals("b972e2edd91e85ec25ec28c29d7dc3e823f28e8a", pushEvent.getAfter());
        Assert.assertEquals("update .flow.yml test test\n", pushEvent.getMessage());
        Assert.assertEquals(
            "https://coding.net/u/benqyang2006/p/flowclibasic/git/commit/b972e2edd91e85ec25ec28c29d7dc3e823f28e8a",
            pushEvent.getHeadCommitUrl());

        Assert.assertEquals("4841b089ecf8...b972e2edd91e", pushEvent.getCompareId());
        Assert.assertEquals("https://coding.net/u/benqyang2006/p/flowclibasic/git/compare/4841b089ecf8...b972e2edd91e",
            pushEvent.getCompareUrl());

        Assert.assertEquals("benqyang2006", pushEvent.getUserId());
        Assert.assertEquals("benqyang2006", pushEvent.getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", pushEvent.getUserEmail());
    }

    @Test
    public void should_convert_to_tag_event_obj() throws Throwable {
        // given:
        String tagEventContent = loadWebhookSampleJson("coding/webhook_tag.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH_OR_TAG);

        // when: build event from header and json content
        GitPushTagEvent tagEvent = (GitPushTagEvent) GitHookEventFactory.build(dummyHeader, tagEventContent);
        Assert.assertNotNull(tagEvent);

        // then:
        Assert.assertEquals(GitEventType.TAG, tagEvent.getType());
        Assert.assertEquals(GitSource.CODING, tagEvent.getGitSource());

        Assert.assertEquals("refs/tags/v1.0", tagEvent.getRef());
        Assert.assertEquals("0000000000000000000000000000000000000000", tagEvent.getBefore());
        Assert.assertEquals("b972e2edd91e85ec25ec28c29d7dc3e823f28e8a", tagEvent.getAfter());

        Assert.assertEquals("benqyang2006", tagEvent.getUserId());
        Assert.assertEquals("benqyang2006", tagEvent.getUsername());
        Assert.assertEquals(null, tagEvent.getUserEmail());

        Assert.assertEquals("b972e2edd91e...v1.0", tagEvent.getCompareId());
        Assert.assertEquals("https://coding.net/u/benqyang2006/p/flowclibasic/git/compare/b972e2edd91e...v1.0",
            tagEvent.getCompareUrl());

        Assert.assertEquals(
            "https://coding.net/u/benqyang2006/p/flowclibasic/git/commit/b972e2edd91e85ec25ec28c29d7dc3e823f28e8a",
            tagEvent.getHeadCommitUrl());
    }

    @Test
    public void should_convert_to_pr_open_event_obj() throws Throwable {
        // given:
        String mrEventContent = loadWebhookSampleJson("coding/webhook_pr_open.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PR);

        // when: build event from header and json content
        GitPullRequestEvent prEvent = (GitPullRequestEvent) GitHookEventFactory.build(dummyHeader, mrEventContent);
        Assert.assertNotNull(prEvent);

        // then:
        Assert.assertEquals(GitSource.CODING, prEvent.getGitSource());
        Assert.assertEquals(GitEventType.PR, prEvent.getType());

        Assert.assertEquals(PullRequestAdapter.STATE_OPEN, prEvent.getAction());
        Assert.assertEquals(State.OPEN, prEvent.getState());
        Assert.assertEquals("test pr", prEvent.getTitle());
        Assert.assertEquals("\u003cp\u003ehello this is the PR test\u003c/p\u003e", prEvent.getDescription());
        Assert.assertEquals("https://coding.net/u/benqyang2006/p/flowclibasic/git/merge/1", prEvent.getUrl());
        Assert.assertEquals(924870, prEvent.getRequestId().intValue());
        Assert.assertEquals("benqyang2006", prEvent.getSubmitter());
        Assert.assertNull(prEvent.getMergedBy());

        GitPullRequestInfo source = prEvent.getSource();
        Assert.assertEquals("develop", source.getBranch());
        Assert.assertEquals("", source.getSha());
        Assert.assertEquals(1243975, source.getProjectId().intValue());
        Assert.assertEquals("flowclibasic", source.getProjectName());

        GitPullRequestInfo target = prEvent.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals("", target.getSha());
        Assert.assertEquals(1243975, target.getProjectId().intValue());
        Assert.assertEquals("flowclibasic", target.getProjectName());
    }

    @Test
    public void should_convert_to_pr_close_obj() throws Throwable {
        // given:
        String mrEventContent = loadWebhookSampleJson("coding/webhook_pr_close.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PR);

        // when: build event from header and json content
        GitPullRequestEvent prEvent = (GitPullRequestEvent) GitHookEventFactory.build(dummyHeader, mrEventContent);
        Assert.assertNotNull(prEvent);

        // then:
        Assert.assertEquals(GitSource.CODING, prEvent.getGitSource());
        Assert.assertEquals(GitEventType.PR, prEvent.getType());

        Assert.assertEquals(PullRequestAdapter.STATE_CLOSE, prEvent.getAction());
        Assert.assertEquals(State.CLOSE, prEvent.getState());
        Assert.assertEquals("23123123", prEvent.getTitle());
        Assert.assertEquals("<p>12312321</p>", prEvent.getDescription());
        Assert.assertEquals(924984, prEvent.getRequestId().intValue());
        Assert.assertEquals("https://coding.net/u/benqyang2006/p/flowclibasic/git/merge/5", prEvent.getUrl());

        Assert.assertNull(prEvent.getSubmitter());
        Assert.assertEquals("benqyang2006", prEvent.getMergedBy());

        GitPullRequestInfo source = prEvent.getSource();
        Assert.assertEquals("develop", source.getBranch());
        Assert.assertEquals("db2da0ba563a323614f3d974a67a2c63e2929177", source.getSha());
        Assert.assertEquals(1243975, source.getProjectId().intValue());
        Assert.assertEquals("flowclibasic", source.getProjectName());

        GitPullRequestInfo target = prEvent.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals("985ae638e0cc8bd4dc2cd8c7a8a6800b00b33997", target.getSha());
        Assert.assertEquals(1243975, target.getProjectId().intValue());
        Assert.assertEquals("flowclibasic", target.getProjectName());
    }

    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitLabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }

}
