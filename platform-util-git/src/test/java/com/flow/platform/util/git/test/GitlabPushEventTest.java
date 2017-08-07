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

import com.flow.platform.util.git.events.GitEventCommit;
import com.flow.platform.util.git.events.GitPushEvent;
import com.google.common.io.Files;
import com.google.gson.Gson;
import java.net.URL;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class GitlabPushEventTest {

    private final static Gson GSON = new Gson();

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        URL resource = getClass().getClassLoader().getResource("gitlab/webhook_push.json");
        String pushEventContent = Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));

        // when:
        GitPushEvent pushEvent = GSON.fromJson(pushEventContent, GitPushEvent.class);
        Assert.assertNotNull(pushEvent);

        // then: verify push info
        Assert.assertEquals("push", pushEvent.getType());
        Assert.assertEquals("95790bf891e76fee5e1747ab589903a6a1f80f22", pushEvent.getBefore());
        Assert.assertEquals("da1560886d4f094c3e6c9ef40349f7d38b5d27d7", pushEvent.getAfter());
        Assert.assertEquals("refs/heads/master", pushEvent.getRef());
        Assert.assertEquals(4, Integer.parseInt(pushEvent.getUserId()));
        Assert.assertEquals("John Smith", pushEvent.getUsername());
        Assert.assertEquals(new Integer(4), pushEvent.getNumOfCommits());

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
}
