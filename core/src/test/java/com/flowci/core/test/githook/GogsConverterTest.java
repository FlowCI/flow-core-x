/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.test.githook;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.githook.converter.GogsConverter;
import com.flowci.core.githook.converter.TriggerConverter;
import com.flowci.core.githook.domain.GitPrTrigger;
import com.flowci.core.githook.domain.GitPushTrigger;
import com.flowci.core.githook.domain.GitTrigger;
import com.flowci.core.githook.domain.GitUser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

public class GogsConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gogsConverter;

    @Test
    public void should_get_push_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_push.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

//        GitPushTrigger trigger = (GitPushTrigger) optional.get();
//        Assert.assertEquals("master", trigger.getRef());
//        Assert.assertEquals("62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69", trigger.getCommitId());
//        Assert.assertEquals("Update 'README.md'\n\nhello\n", trigger.getMessage());
//        Assert.assertEquals(
//                "http://localhost:3000/test/my-first-repo/commit/62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69",
//                trigger.getCommitUrl());
//        Assert.assertEquals("2019-10-03T10:44:15Z", trigger.getTime());
//        Assert.assertEquals(1, trigger.getNumOfCommit());
//
//        GitUser pusher = trigger.getAuthor();
//        Assert.assertEquals("benqyang_2006@gogs.test", pusher.getEmail());
//        Assert.assertEquals("test", pusher.getUsername());
//        Assert.assertEquals(
//                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
//                pusher.getAvatarLink());
    }

    @Test
    public void should_get_tag_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_tag.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger tag = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, tag.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.TAG, tag.getEvent());

//        Assert.assertEquals("v4.0", tag.getRef());
//        Assert.assertEquals("4", tag.getCommitId());
//        Assert.assertEquals("2019-10-03T12:46:57Z", tag.getTime());
//        Assert.assertEquals("title for v4.0", tag.getMessage());
//        Assert.assertEquals("", tag.getCommitUrl());
//        Assert.assertEquals(0, tag.getNumOfCommit());
//
//        GitUser author = tag.getAuthor();
//        Assert.assertEquals("test", author.getUsername());
//        Assert.assertEquals("benqyang_2006@gogs.com", author.getEmail());
//        Assert.assertEquals(
//                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
//                author.getAvatarLink());
    }

    @Test
    public void should_get_mr_open_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_opened.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, trigger.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, trigger.getEvent());

        Assert.assertEquals("1", trigger.getNumber());
        Assert.assertEquals("first pr test", trigger.getTitle());
        Assert.assertEquals("test content..", trigger.getBody());
        Assert.assertEquals("", trigger.getTime());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo/pulls/1", trigger.getUrl());
        Assert.assertEquals("", trigger.getNumOfCommits());
        Assert.assertEquals("", trigger.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, trigger.getMerged());

        GitPrTrigger.Source head = trigger.getHead();
        Assert.assertEquals("", head.getCommit());
        Assert.assertEquals("develop", head.getRef());
        Assert.assertEquals("test/my-first-repo", head.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = trigger.getBase();
        Assert.assertEquals("", base.getCommit());
        Assert.assertEquals("master", base.getRef());
        Assert.assertEquals("test/my-first-repo", base.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = trigger.getSender();
        Assert.assertEquals("1", sender.getId());
        Assert.assertEquals("test", sender.getUsername());
        Assert.assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());
    }

    @Test
    public void should_get_mr_merged_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_merged.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, trigger.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PR_MERGED, trigger.getEvent());

        Assert.assertEquals("4", trigger.getNumber());
        Assert.assertEquals("second test pr", trigger.getTitle());
        Assert.assertEquals("pr content..", trigger.getBody());
        Assert.assertEquals("2019-10-03T12:41:32.249138587Z", trigger.getTime());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo/pulls/4", trigger.getUrl());
        Assert.assertEquals("", trigger.getNumOfCommits());
        Assert.assertEquals("", trigger.getNumOfFileChanges());
        Assert.assertEquals(Boolean.TRUE, trigger.getMerged());

        GitPrTrigger.Source head = trigger.getHead();
        Assert.assertEquals("", head.getCommit());
        Assert.assertEquals("develop", head.getRef());
        Assert.assertEquals("test/my-first-repo", head.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = trigger.getBase();
        Assert.assertEquals("", base.getCommit());
        Assert.assertEquals("master", base.getRef());
        Assert.assertEquals("test/my-first-repo", base.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = trigger.getSender();
        Assert.assertEquals("1", sender.getId());
        Assert.assertEquals("test", sender.getUsername());
        Assert.assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());
    }
}
