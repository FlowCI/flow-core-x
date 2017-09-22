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
import com.flow.platform.util.git.hooks.GitLabEvents.Hooks;
import com.flow.platform.util.git.model.GitEventCommit;
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
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class GitlabHooksEventTest {

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given: init dummy data of http header and push event content
        String pushEventContent = loadWebhookSampleJson("gitlab/webhook_push.json");
        Map<String, String> mockHeader = new HashMap<>();
        mockHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);

        // when:
        GitPushTagEvent pushEvent = (GitPushTagEvent) GitHookEventFactory.build(mockHeader, pushEventContent);
        Assert.assertNotNull(pushEvent);

        // then: verify push info
        Assert.assertEquals(GitSource.GITLAB, pushEvent.getGitSource());
        Assert.assertEquals(GitEventType.PUSH, pushEvent.getType());

        Assert.assertEquals("95790bf891e76fee5e1747ab589903a6a1f80f22", pushEvent.getBefore());
        Assert.assertEquals("da1560886d4f094c3e6c9ef40349f7d38b5d27d7", pushEvent.getAfter());
        Assert.assertEquals("refs/heads/master", pushEvent.getRef());
        Assert.assertEquals(4, Integer.parseInt(pushEvent.getUserId()));
        Assert.assertEquals("John Smith", pushEvent.getUsername());

        // then: verify push commit info
        List<GitEventCommit> commits = pushEvent.getCommits();
        Assert.assertEquals(2, commits.size());

        GitEventCommit firstCommit = commits.get(0);
        Assert.assertEquals("b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327", firstCommit.getId());
        Assert.assertEquals("Update Catalan translation to e38cb41.", firstCommit.getMessage());
        Assert.assertEquals("2011-12-12T14:27:31+02:00", firstCommit.getTimestamp());

        Assert.assertEquals("Jordi Mallach", firstCommit.getAuthor().getName());
        Assert.assertEquals("jordi@softcatala.org", firstCommit.getAuthor().getEmail());

        Assert.assertEquals("CHANGELOG", firstCommit.getAdded().get(0));
        Assert.assertEquals("app/controller/application.rb", firstCommit.getModified().get(0));
    }

    @Test
    public void should_convert_to_tag_event_obj() throws Throwable {
        // given:
        String tagEventContent = loadWebhookSampleJson("gitlab/webhook_tag.json");
        Map<String, String> mockHeader = new HashMap<>();
        mockHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_TAG);

        // when:
        GitPushTagEvent tagEvent = (GitPushTagEvent) GitHookEventFactory.build(mockHeader, tagEventContent);
        Assert.assertNotNull(tagEvent);

        // then:
        Assert.assertEquals(GitSource.GITLAB, tagEvent.getGitSource());
        Assert.assertEquals(GitEventType.TAG, tagEvent.getType());
        Assert.assertEquals("0000000000000000000000000000000000000000", tagEvent.getBefore());
        Assert.assertEquals("82b3d5ae55f7080f1e6022629cdb57bfae7cccc7", tagEvent.getAfter());
        Assert.assertEquals("refs/tags/v1.0.0", tagEvent.getRef());
        Assert.assertEquals(1, Integer.parseInt(tagEvent.getUserId()));
        Assert.assertEquals("John Smith", tagEvent.getUsername());
        Assert.assertEquals(0, tagEvent.getCommits().size());
        Assert.assertEquals("hello test", tagEvent.getMessage());
    }

    @Test
    public void should_convert_to_pr_event_obj() throws Throwable {
        // given:
        String prEventContent = loadWebhookSampleJson("gitlab/webhook_mr.json");
        Map<String, String> mockHeader = new HashMap<>();
        mockHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_MR);

        // when:
        GitPullRequestEvent mrEvent = (GitPullRequestEvent) GitHookEventFactory.build(mockHeader, prEventContent);
        Assert.assertNotNull(mrEvent);

        // then:
        Assert.assertEquals(GitSource.GITLAB, mrEvent.getGitSource());
        Assert.assertEquals(GitEventType.MR, mrEvent.getType());

        Assert.assertEquals(99, mrEvent.getRequestId().intValue());
        Assert.assertEquals("MS-Viewport", mrEvent.getTitle());
        Assert.assertEquals("opened", mrEvent.getStatus());
        Assert.assertEquals("open", mrEvent.getAction());

        GitPullRequestInfo target = mrEvent.getTarget();
        Assert.assertEquals("master", target.getBranch());
        Assert.assertEquals(14, target.getProjectId().intValue());

        GitPullRequestInfo source = mrEvent.getSource();
        Assert.assertEquals("ms-viewport", source.getBranch());
        Assert.assertEquals(14, source.getProjectId().intValue());
        Assert.assertEquals("da1560886d4f094c3e6c9ef40349f7d38b5d27d7", source.getSha());
    }

    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitlabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
