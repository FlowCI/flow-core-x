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

package com.flowci.core.test.git;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.converter.GogsConverter;
import com.flowci.core.git.converter.TriggerConverter;
import com.flowci.core.git.domain.*;
import com.flowci.core.test.SpringScenario;
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

        GitPushTrigger t = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        Assert.assertEquals(GitSource.GOGS, t.getSource());
        Assert.assertEquals("1", t.getRepoId());
        Assert.assertEquals("master", t.getRef());
        Assert.assertEquals("Update 'README.md'\n\nhello\n", t.getMessage());
        Assert.assertEquals(1, t.getNumOfCommit());
        Assert.assertEquals("test", t.getSender().getName());
        Assert.assertEquals("benqyang_2006@gogs.test", t.getSender().getEmail());

        var commit = t.getCommits().get(0);
        Assert.assertEquals("Update 'README.md'\n\nhello\n", commit.getMessage());
        Assert.assertEquals("2019-10-03T10:44:15Z", commit.getTime());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo/commit/62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69", commit.getUrl());
        Assert.assertEquals("test", commit.getAuthor().getName());
        Assert.assertEquals("benqyang_2006@gogs.test", commit.getAuthor().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_tag_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_tag.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, t.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.TAG, t.getEvent());
        Assert.assertEquals("1", t.getRepoId());
        Assert.assertEquals("v4.0", t.getRef());
        Assert.assertEquals("title for v4.0\n4.0 content", t.getMessage());
        Assert.assertEquals("test", t.getSender().getName());
        Assert.assertEquals("benqyang_2006@gogs.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("v4.0", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_mr_open_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_opened.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, t.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());

        Assert.assertEquals("1", t.getNumber());
        Assert.assertEquals("first pr test", t.getTitle());
        Assert.assertEquals("test content..", t.getBody());
        Assert.assertEquals("", t.getTime());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo/pulls/1", t.getUrl());
        Assert.assertEquals("", t.getNumOfCommits());
        Assert.assertEquals("", t.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, t.getMerged());

        GitPrTrigger.Source head = t.getHead();
        Assert.assertEquals("", head.getCommit());
        Assert.assertEquals("develop", head.getRef());
        Assert.assertEquals("test/my-first-repo", head.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = t.getBase();
        Assert.assertEquals("", base.getCommit());
        Assert.assertEquals("master", base.getRef());
        Assert.assertEquals("test/my-first-repo", base.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = t.getSender();
        Assert.assertEquals("1", sender.getId());
        Assert.assertEquals("test", sender.getUsername());
        Assert.assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());

        var vars = t.toVariableMap();
        Assert.assertEquals("develop", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_mr_merged_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_merged.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitSource.GOGS, t.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PR_MERGED, t.getEvent());

        Assert.assertEquals("4", t.getNumber());
        Assert.assertEquals("second test pr", t.getTitle());
        Assert.assertEquals("pr content..", t.getBody());
        Assert.assertEquals("2019-10-03T12:41:32.249138587Z", t.getTime());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo/pulls/4", t.getUrl());
        Assert.assertEquals("", t.getNumOfCommits());
        Assert.assertEquals("", t.getNumOfFileChanges());
        Assert.assertEquals(Boolean.TRUE, t.getMerged());

        GitPrTrigger.Source head = t.getHead();
        Assert.assertEquals("", head.getCommit());
        Assert.assertEquals("develop", head.getRef());
        Assert.assertEquals("test/my-first-repo", head.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = t.getBase();
        Assert.assertEquals("", base.getCommit());
        Assert.assertEquals("master", base.getRef());
        Assert.assertEquals("test/my-first-repo", base.getRepoName());
        Assert.assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = t.getSender();
        Assert.assertEquals("1", sender.getId());
        Assert.assertEquals("test", sender.getUsername());
        Assert.assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
