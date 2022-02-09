/*
 * Copyright 2020 flow.ci
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
import com.flowci.core.git.hook.domain.*;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.git.hook.converter.GiteeConverter;
import com.flowci.core.git.hook.converter.TriggerConverter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

public class GiteeConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter giteeConverter;

    @Test
    public void should_parse_ping_event() {
        InputStream stream = load("gitee/webhook_ping.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Ping, stream);
        Assert.assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        Assert.assertNotNull(trigger);
    }

    @Test
    public void should_parse_push_event() {
        InputStream stream = load("gitee/webhook_push.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitSource.GITEE, t.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        Assert.assertEquals(1, t.getNumOfCommit());
        Assert.assertEquals("2775902", t.getRepoId());
        Assert.assertEquals("update README.md.\ntest pr message..", t.getMessage());
        Assert.assertEquals("feature/222", t.getRef());
        Assert.assertEquals("yang.guo", t.getSender().getName());
        Assert.assertEquals("benqyang_2006@hotmail.com", t.getSender().getEmail());
        Assert.assertEquals("gy2006", t.getSender().getUsername());

        var commit = t.getCommits().get(0);
        Assert.assertEquals("ea926aebbe8738e903345534a9b158716b904816", commit.getId());
        Assert.assertEquals("update README.md.\ntest pr message..", commit.getMessage());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/commit/ea926aebbe8738e903345534a9b158716b904816", commit.getUrl());
        Assert.assertEquals("2020-02-25T22:52:24+08:00", commit.getTime());

        Assert.assertEquals("gy2006", commit.getAuthor().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", commit.getAuthor().getEmail());
        Assert.assertEquals("yang.guo", commit.getAuthor().getName());

        var vars = t.toVariableMap();
        Assert.assertEquals("feature/222", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_tag_event() {
        InputStream stream = load("gitee/webhook_tag.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitTagTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        Assert.assertNotNull(t);
        Assert.assertEquals(GitSource.GITEE, t.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.TAG, t.getEvent());
        Assert.assertEquals("2775902", t.getRepoId());
        Assert.assertEquals("v0.1", t.getRef());
        Assert.assertEquals("Initial commit", t.getMessage());
        Assert.assertEquals("yang.guo", t.getSender().getName());

        var vars = t.toVariableMap();
        Assert.assertEquals("v0.1", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_pr_open_event() {
        InputStream stream = load("gitee/webhook_pr_open.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertNotNull(t);
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());
        Assert.assertEquals(GitSource.GITEE, t.getSource());

        Assert.assertEquals("1", t.getNumber());
        Assert.assertEquals("gitee create pr test", t.getTitle());
        Assert.assertEquals("pr comments...\r\n1.aa\r\n2.bb\r\n3.cc", t.getBody());
        Assert.assertEquals("2020-02-25T22:53:47+08:00", t.getTime());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/pulls/1", t.getUrl());
        Assert.assertEquals("1", t.getNumOfCommits());
        Assert.assertEquals("1", t.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, t.getMerged());

        // verify head repo
        Assert.assertEquals("ea926aebbe8738e903345534a9b158716b904816", t.getHead().getCommit());
        Assert.assertEquals("feature/222", t.getHead().getRef());
        Assert.assertEquals("gy2006/flow-test", t.getHead().getRepoName());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test", t.getHead().getRepoUrl());

        // verify base repo
        Assert.assertEquals("2e6e071da3f8c718d0969f3d96cedc848dab605e", t.getBase().getCommit());
        Assert.assertEquals("master", t.getBase().getRef());
        Assert.assertEquals("gy2006/flow-test", t.getBase().getRepoName());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test", t.getBase().getRepoUrl());

        Assert.assertEquals("1666376", t.getSender().getId());
        Assert.assertEquals("yang.guo", t.getSender().getName());
        Assert.assertEquals("gy2006", t.getSender().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("feature/222", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_pr_merge_event() {
        InputStream stream = load("gitee/webhook_pr_merge.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertNotNull(t);
        Assert.assertEquals(GitTrigger.GitEvent.PR_MERGED, t.getEvent());
        Assert.assertEquals(GitSource.GITEE, t.getSource());
        Assert.assertEquals(Boolean.TRUE, t.getMerged());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
