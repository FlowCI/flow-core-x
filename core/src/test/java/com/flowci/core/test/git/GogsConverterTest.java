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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GogsConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gogsConverter;

    @Test
    void should_get_push_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_push.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Push, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        assertEquals(GitSource.GOGS, t.getSource());
        assertEquals("1", t.getRepoId());
        assertEquals("master", t.getRef());
        assertEquals("Update 'README.md'\n\nhello\n", t.getMessage());
        assertEquals(1, t.getNumOfCommit());
        assertEquals("test", t.getSender().getName());
        assertEquals("benqyang_2006@gogs.test", t.getSender().getEmail());

        var commit = t.getCommits().get(0);
        assertEquals("Update 'README.md'\n\nhello\n", commit.getMessage());
        assertEquals("2019-10-03T10:44:15Z", commit.getTime());
        assertEquals("http://localhost:3000/test/my-first-repo/commit/62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69", commit.getUrl());
        assertEquals("test", commit.getAuthor().getName());
        assertEquals("benqyang_2006@gogs.test", commit.getAuthor().getEmail());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_tag_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_tag.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Tag, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        assertEquals(GitSource.GOGS, t.getSource());
        assertEquals(GitTrigger.GitEvent.TAG, t.getEvent());
        assertEquals("1", t.getRepoId());
        assertEquals("v4.0", t.getRef());
        assertEquals("title for v4.0\n4.0 content", t.getMessage());
        assertEquals("test", t.getSender().getName());
        assertEquals("benqyang_2006@gogs.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        assertEquals("v4.0", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_mr_open_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_opened.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertEquals(GitSource.GOGS, t.getSource());
        assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());

        assertEquals("1", t.getNumber());
        assertEquals("first pr test", t.getTitle());
        assertEquals("test content..", t.getBody());
        assertEquals("", t.getTime());
        assertEquals("http://localhost:3000/test/my-first-repo/pulls/1", t.getUrl());
        assertEquals("", t.getNumOfCommits());
        assertEquals("", t.getNumOfFileChanges());
        assertEquals(Boolean.FALSE, t.getMerged());

        GitPrTrigger.Source head = t.getHead();
        assertEquals("", head.getCommit());
        assertEquals("develop", head.getRef());
        assertEquals("test/my-first-repo", head.getRepoName());
        assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = t.getBase();
        assertEquals("", base.getCommit());
        assertEquals("master", base.getRef());
        assertEquals("test/my-first-repo", base.getRepoName());
        assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = t.getSender();
        assertEquals("1", sender.getId());
        assertEquals("test", sender.getUsername());
        assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());

        var vars = t.toVariableMap();
        assertEquals("develop", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_mr_merged_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_pr_merged.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.PR, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertEquals(GitSource.GOGS, t.getSource());
        assertEquals(GitTrigger.GitEvent.PR_MERGED, t.getEvent());

        assertEquals("4", t.getNumber());
        assertEquals("second test pr", t.getTitle());
        assertEquals("pr content..", t.getBody());
        assertEquals("2019-10-03T12:41:32.249138587Z", t.getTime());
        assertEquals("http://localhost:3000/test/my-first-repo/pulls/4", t.getUrl());
        assertEquals("", t.getNumOfCommits());
        assertEquals("", t.getNumOfFileChanges());
        assertEquals(Boolean.TRUE, t.getMerged());

        GitPrTrigger.Source head = t.getHead();
        assertEquals("", head.getCommit());
        assertEquals("develop", head.getRef());
        assertEquals("test/my-first-repo", head.getRepoName());
        assertEquals("http://localhost:3000/test/my-first-repo", head.getRepoUrl());

        GitPrTrigger.Source base = t.getBase();
        assertEquals("", base.getCommit());
        assertEquals("master", base.getRef());
        assertEquals("test/my-first-repo", base.getRepoName());
        assertEquals("http://localhost:3000/test/my-first-repo", base.getRepoUrl());

        GitUser sender = t.getSender();
        assertEquals("1", sender.getId());
        assertEquals("test", sender.getUsername());
        assertEquals("benqyang_2006@gogs.test", sender.getEmail());
        assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                sender.getAvatarLink());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
