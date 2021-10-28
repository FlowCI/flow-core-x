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
import com.flowci.core.test.SpringScenario;
import com.flowci.core.githook.converter.GiteeConverter;
import com.flowci.core.githook.converter.TriggerConverter;
import com.flowci.core.githook.domain.GitPingTrigger;
import com.flowci.core.githook.domain.GitPrTrigger;
import com.flowci.core.githook.domain.GitPushTrigger;
import com.flowci.core.githook.domain.GitTrigger;
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

        GitPushTrigger trigger = (GitPushTrigger) optional.get();
        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitSource.GITEE, trigger.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.PUSH, trigger.getEvent());

        Assert.assertEquals("feature/222", trigger.getRef());
        Assert.assertEquals("ea926aebbe8738e903345534a9b158716b904816", trigger.getCommitId());
        Assert.assertEquals("update README.md.\ntest pr message..", trigger.getMessage());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/commit/ea926aebbe8738e903345534a9b158716b904816", trigger.getCommitUrl());
        Assert.assertEquals("2020-02-25T22:52:24+08:00", trigger.getTime());
        Assert.assertEquals(1, trigger.getNumOfCommit());

        Assert.assertEquals("gy2006", trigger.getAuthor().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", trigger.getAuthor().getEmail());
        Assert.assertEquals("yang.guo", trigger.getAuthor().getName());
        Assert.assertEquals("https://gitee.com/assets/no_portrait.png", trigger.getAuthor().getAvatarLink());
    }

    @Test
    public void should_parse_tag_event() {
        InputStream stream = load("gitee/webhook_tag.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());

        GitPushTrigger trigger = (GitPushTrigger) optional.get();
        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitSource.GITEE, trigger.getSource());
        Assert.assertEquals(GitTrigger.GitEvent.TAG, trigger.getEvent());

        Assert.assertEquals("v0.1", trigger.getRef());
        Assert.assertEquals("2e6e071da3f8c718d0969f3d96cedc848dab605e", trigger.getCommitId());
        Assert.assertEquals("Initial commit", trigger.getMessage());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/commit/2e6e071da3f8c718d0969f3d96cedc848dab605e", trigger.getCommitUrl());
        Assert.assertEquals("2017-12-04T09:52:25+08:00", trigger.getTime());
        Assert.assertEquals(0, trigger.getNumOfCommit());

        Assert.assertEquals("gy2006", trigger.getAuthor().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", trigger.getAuthor().getEmail());
        Assert.assertEquals("yang.guo", trigger.getAuthor().getName());
        Assert.assertEquals("https://gitee.com/assets/no_portrait.png", trigger.getAuthor().getAvatarLink());
    }

    @Test
    public void should_parse_pr_open_event() {
        InputStream stream = load("gitee/webhook_pr_open.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);
        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, trigger.getEvent());
        Assert.assertEquals(GitSource.GITEE, trigger.getSource());

        Assert.assertEquals("1", trigger.getNumber());
        Assert.assertEquals("gitee create pr test", trigger.getTitle());
        Assert.assertEquals("pr comments...\r\n1.aa\r\n2.bb\r\n3.cc", trigger.getBody());
        Assert.assertEquals("2020-02-25T22:53:47+08:00", trigger.getTime());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/pulls/1", trigger.getUrl());
        Assert.assertEquals("1", trigger.getNumOfCommits());
        Assert.assertEquals("1", trigger.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, trigger.getMerged());

        // verify head repo
        Assert.assertEquals("ea926aebbe8738e903345534a9b158716b904816", trigger.getHead().getCommit());
        Assert.assertEquals("feature/222", trigger.getHead().getRef());
        Assert.assertEquals("gy2006/flow-test", trigger.getHead().getRepoName());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test", trigger.getHead().getRepoUrl());

        // verify base repo
        Assert.assertEquals("2e6e071da3f8c718d0969f3d96cedc848dab605e", trigger.getBase().getCommit());
        Assert.assertEquals("master", trigger.getBase().getRef());
        Assert.assertEquals("gy2006/flow-test", trigger.getBase().getRepoName());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test", trigger.getBase().getRepoUrl());

        Assert.assertEquals("1666376", trigger.getSender().getId());
        Assert.assertEquals("yang.guo", trigger.getSender().getName());
        Assert.assertEquals("gy2006", trigger.getSender().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", trigger.getSender().getEmail());
    }

    @Test
    public void should_parse_pr_merge_event() {
        InputStream stream = load("gitee/webhook_pr_merge.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);
        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitTrigger.GitEvent.PR_MERGED, trigger.getEvent());
        Assert.assertEquals(GitSource.GITEE, trigger.getSource());
        Assert.assertEquals(Boolean.TRUE, trigger.getMerged());
    }
}
