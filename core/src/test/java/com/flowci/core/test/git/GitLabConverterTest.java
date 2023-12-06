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

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.converter.GitLabConverter;
import com.flowci.core.git.converter.TriggerConverter;
import com.flowci.core.git.domain.*;
import com.flowci.core.git.domain.GitTrigger.GitEvent;
import com.flowci.core.test.SpringScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GitLabConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitLabConverter;

    @Test
    void should_get_push_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_push.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Push, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        assertEquals(GitSource.GITLAB, t.getSource());
        assertEquals("2048650", t.getRepoId());
        assertEquals(3, t.getNumOfCommit());
        assertEquals("master", t.getRef());
        assertEquals("Update .flow.yml test", t.getMessage());
        assertEquals("yang.guo", t.getSender().getName());

        // check first commit
        var commit1 = t.getCommits().get(0);
        assertEquals("d8e7334543d437c1a889a9187e66d1968280d7d4", commit1.getId());
        assertEquals("Update .flow.yml test", commit1.getMessage());
        assertEquals("2017-10-17T08:23:36Z", commit1.getTime());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/d8e7334543d437c1a889a9187e66d1968280d7d4", commit1.getUrl());
        assertEquals("yang.guo", commit1.getAuthor().getName());
        assertEquals("benqyang_2006@hotmail.com", commit1.getAuthor().getEmail());

        // check second commit
        var commit2 = t.getCommits().get(1);
        assertEquals("0c0726be026a9fec991d7c3f64c2c3fc6babed8c", commit2.getId());
        assertEquals("Update .flow.yml", commit2.getMessage());
        assertEquals("2017-10-17T08:16:21Z", commit2.getTime());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/0c0726be026a9fec991d7c3f64c2c3fc6babed8c", commit2.getUrl());
        assertEquals("yang.guo", commit2.getAuthor().getName());
        assertEquals("benqyang_2006@hotmail.com", commit2.getAuthor().getEmail());

        // check third commit
        var commit3 = t.getCommits().get(2);
        assertEquals("55ef9a6330eecd15132f9ff35e4f8664eb254e88", commit3.getId());
        assertEquals("Update .flow.yml add", commit3.getMessage());
        assertEquals("2017-10-17T07:57:44Z", commit3.getTime());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/55ef9a6330eecd15132f9ff35e4f8664eb254e88", commit3.getUrl());
        assertEquals("yang.guo", commit3.getAuthor().getName());
        assertEquals("benqyang_2006@hotmail.com", commit3.getAuthor().getEmail());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_tag_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_tag.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Tag, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        assertEquals(GitEvent.TAG, t.getEvent());
        assertEquals(GitSource.GITLAB, t.getSource());
        assertEquals("2048650", t.getRepoId());
        assertEquals(1, t.getNumOfCommit());
        assertEquals("v2.0", t.getRef());
        assertEquals("test tag push", t.getMessage());

        var commit = t.getCommits().get(0);
        assertEquals("1b4d99d54c29a31a92e990e6bac301eea0c1fc94", commit.getId());
        assertEquals("Merge branch 'developer' into 'master'\n\nUpdate package.json title\n\nSee merge request yang-guo-2016/kai-web!1", commit.getMessage());
        assertEquals("yang.guo", commit.getAuthor().getName());
        assertEquals("gy@fir.im", commit.getAuthor().getEmail());

        var vars = t.toVariableMap();
        assertEquals("v2.0", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_pr_open_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_opened.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());
        assertEquals(GitSource.GITLAB, t.getSource());
        assertFalse(t.getMerged());
        assertEquals("Update package.json title", t.getTitle());
        assertEquals("pr message", t.getBody());
        assertEquals("2017-08-08T08:44:54.622Z", t.getTime());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web/merge_requests/1", t.getUrl());

        GitPrTrigger.Source from = t.getHead();
        assertEquals("kai-web", from.getRepoName());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web", from.getRepoUrl());
        assertEquals("developer", from.getRef());
        assertEquals("9e81037427cc1c50641c5ffc7b6c70a487886ed8", from.getCommit());

        GitPrTrigger.Source to = t.getBase();
        assertEquals("kai-web", to.getRepoName());
        assertEquals("https://gitlab.com/yang-guo-2016/kai-web", to.getRepoUrl());
        assertEquals("master", to.getRef());
        assertEquals("", to.getCommit());

        GitUser sender = t.getSender();
        assertEquals(StringHelper.EMPTY, sender.getEmail());
        assertEquals("yang-guo-2016", sender.getUsername());
        assertEquals(
                "https://secure.gravatar.com/avatar/25fc63da4f632d2a2c10724cba3b9efc?s=80\u0026d=identicon",
                sender.getAvatarLink());

        var vars = t.toVariableMap();
        assertEquals("developer", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_get_pr_close_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_merged.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertEquals(GitEvent.PR_MERGED, t.getEvent());
        assertEquals(GitSource.GITLAB, t.getSource());
        assertTrue(t.getMerged());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
