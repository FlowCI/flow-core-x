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
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.hook.converter.GitLabConverter;
import com.flowci.core.git.hook.converter.TriggerConverter;
import com.flowci.core.git.hook.domain.*;
import com.flowci.core.git.hook.domain.GitTrigger.GitEvent;
import com.flowci.core.test.SpringScenario;
import com.flowci.util.StringHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

public class GitLabConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitLabConverter;

    @Test
    public void should_get_push_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_push.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        Assert.assertEquals(GitSource.GITLAB, t.getSource());
        Assert.assertEquals("2048650", t.getRepoId());
        Assert.assertEquals(3, t.getNumOfCommit());
        Assert.assertEquals("master", t.getRef());
        Assert.assertEquals("Update .flow.yml test", t.getMessage());
        Assert.assertEquals("yang.guo", t.getSender().getName());

        // check first commit
        var commit1 = t.getCommits().get(0);
        Assert.assertEquals("d8e7334543d437c1a889a9187e66d1968280d7d4", commit1.getId());
        Assert.assertEquals("Update .flow.yml test", commit1.getMessage());
        Assert.assertEquals("2017-10-17T08:23:36Z", commit1.getTime());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/d8e7334543d437c1a889a9187e66d1968280d7d4", commit1.getUrl());
        Assert.assertEquals("yang.guo", commit1.getAuthor().getName());
        Assert.assertEquals("benqyang_2006@hotmail.com", commit1.getAuthor().getEmail());

        // check second commit
        var commit2 = t.getCommits().get(1);
        Assert.assertEquals("0c0726be026a9fec991d7c3f64c2c3fc6babed8c", commit2.getId());
        Assert.assertEquals("Update .flow.yml", commit2.getMessage());
        Assert.assertEquals("2017-10-17T08:16:21Z", commit2.getTime());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/0c0726be026a9fec991d7c3f64c2c3fc6babed8c", commit2.getUrl());
        Assert.assertEquals("yang.guo", commit2.getAuthor().getName());
        Assert.assertEquals("benqyang_2006@hotmail.com", commit2.getAuthor().getEmail());

        // check third commit
        var commit3 = t.getCommits().get(2);
        Assert.assertEquals("55ef9a6330eecd15132f9ff35e4f8664eb254e88", commit3.getId());
        Assert.assertEquals("Update .flow.yml add", commit3.getMessage());
        Assert.assertEquals("2017-10-17T07:57:44Z", commit3.getTime());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web/commit/55ef9a6330eecd15132f9ff35e4f8664eb254e88", commit3.getUrl());
        Assert.assertEquals("yang.guo", commit3.getAuthor().getName());
        Assert.assertEquals("benqyang_2006@hotmail.com", commit3.getAuthor().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_tag_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_tag.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        Assert.assertEquals(GitEvent.TAG, t.getEvent());
        Assert.assertEquals(GitSource.GITLAB, t.getSource());
        Assert.assertEquals("2048650", t.getRepoId());
        Assert.assertEquals(1, t.getNumOfCommit());
        Assert.assertEquals("v2.0", t.getRef());
        Assert.assertEquals("test tag push", t.getMessage());

        var commit = t.getCommits().get(0);
        Assert.assertEquals("1b4d99d54c29a31a92e990e6bac301eea0c1fc94", commit.getId());
        Assert.assertEquals("Merge branch 'developer' into 'master'\n\nUpdate package.json title\n\nSee merge request yang-guo-2016/kai-web!1", commit.getMessage());
        Assert.assertEquals("yang.guo", commit.getAuthor().getName());
        Assert.assertEquals("gy@fir.im", commit.getAuthor().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("v2.0", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_pr_open_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_opened.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());
        Assert.assertEquals(GitSource.GITLAB, t.getSource());
        Assert.assertFalse(t.getMerged());
        Assert.assertEquals("Update package.json title", t.getTitle());
        Assert.assertEquals("pr message", t.getBody());
        Assert.assertEquals("2017-08-08T08:44:54.622Z", t.getTime());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web/merge_requests/1", t.getUrl());

        GitPrTrigger.Source from = t.getHead();
        Assert.assertEquals("kai-web", from.getRepoName());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web", from.getRepoUrl());
        Assert.assertEquals("developer", from.getRef());
        Assert.assertEquals("9e81037427cc1c50641c5ffc7b6c70a487886ed8", from.getCommit());

        GitPrTrigger.Source to = t.getBase();
        Assert.assertEquals("kai-web", to.getRepoName());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web", to.getRepoUrl());
        Assert.assertEquals("master", to.getRef());
        Assert.assertEquals("", to.getCommit());

        GitUser sender = t.getSender();
        Assert.assertEquals(StringHelper.EMPTY, sender.getEmail());
        Assert.assertEquals("yang-guo-2016", sender.getUsername());
        Assert.assertEquals(
            "https://secure.gravatar.com/avatar/25fc63da4f632d2a2c10724cba3b9efc?s=80\u0026d=identicon",
            sender.getAvatarLink());

        var vars = t.toVariableMap();
        Assert.assertEquals("developer", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_get_pr_close_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_merged.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitEvent.PR_MERGED, t.getEvent());
        Assert.assertEquals(GitSource.GITLAB, t.getSource());
        Assert.assertTrue(t.getMerged());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
