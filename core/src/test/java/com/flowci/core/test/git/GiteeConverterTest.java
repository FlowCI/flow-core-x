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

package com.flowci.core.test.git;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.domain.*;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.git.converter.GiteeConverter;
import com.flowci.core.git.converter.TriggerConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GiteeConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter giteeConverter;

    @Test
    void should_parse_ping_event() {
        InputStream stream = load("gitee/webhook_ping.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Ping, stream);
        assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        assertNotNull(trigger);
    }

    @Test
    void should_parse_push_event() {
        InputStream stream = load("gitee/webhook_push.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Push, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        assertEquals(GitSource.GITEE, t.getSource());
        assertEquals(GitTrigger.GitEvent.PUSH, t.getEvent());
        assertEquals(1, t.getNumOfCommit());
        assertEquals("2775902", t.getRepoId());
        assertEquals("update README.md.\ntest pr message..", t.getMessage());
        assertEquals("feature/222", t.getRef());
        assertEquals("yang.guo", t.getSender().getName());
        assertEquals("benqyang_2006@hotmail.com", t.getSender().getEmail());
        assertEquals("gy2006", t.getSender().getUsername());

        var commit = t.getCommits().get(0);
        assertEquals("ea926aebbe8738e903345534a9b158716b904816", commit.getId());
        assertEquals("update README.md.\ntest pr message..", commit.getMessage());
        assertEquals("https://gitee.com/gy2006/flow-test/commit/ea926aebbe8738e903345534a9b158716b904816", commit.getUrl());
        assertEquals("2020-02-25T22:52:24+08:00", commit.getTime());

        assertEquals("gy2006", commit.getAuthor().getUsername());
        assertEquals("benqyang_2006@hotmail.com", commit.getAuthor().getEmail());
        assertEquals("yang.guo", commit.getAuthor().getName());

        var vars = t.toVariableMap();
        assertEquals("feature/222", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_tag_event() {
        InputStream stream = load("gitee/webhook_tag.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Tag, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitTagTrigger);

        GitPushTrigger t = (GitPushTrigger) optional.get();
        assertNotNull(t);
        assertEquals(GitSource.GITEE, t.getSource());
        assertEquals(GitTrigger.GitEvent.TAG, t.getEvent());
        assertEquals("2775902", t.getRepoId());
        assertEquals("v0.1", t.getRef());
        assertEquals("Initial commit", t.getMessage());
        assertEquals("yang.guo", t.getSender().getName());

        var vars = t.toVariableMap();
        assertEquals("v0.1", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_pr_open_event() {
        InputStream stream = load("gitee/webhook_pr_open.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertNotNull(t);
        assertEquals(GitTrigger.GitEvent.PR_OPENED, t.getEvent());
        assertEquals(GitSource.GITEE, t.getSource());

        assertEquals("1", t.getNumber());
        assertEquals("gitee create pr test", t.getTitle());
        assertEquals("pr comments...\r\n1.aa\r\n2.bb\r\n3.cc", t.getBody());
        assertEquals("2020-02-25T22:53:47+08:00", t.getTime());
        assertEquals("https://gitee.com/gy2006/flow-test/pulls/1", t.getUrl());
        assertEquals("1", t.getNumOfCommits());
        assertEquals("1", t.getNumOfFileChanges());
        assertEquals(Boolean.FALSE, t.getMerged());

        // verify head repo
        assertEquals("ea926aebbe8738e903345534a9b158716b904816", t.getHead().getCommit());
        assertEquals("feature/222", t.getHead().getRef());
        assertEquals("gy2006/flow-test", t.getHead().getRepoName());
        assertEquals("https://gitee.com/gy2006/flow-test", t.getHead().getRepoUrl());

        // verify base repo
        assertEquals("2e6e071da3f8c718d0969f3d96cedc848dab605e", t.getBase().getCommit());
        assertEquals("master", t.getBase().getRef());
        assertEquals("gy2006/flow-test", t.getBase().getRepoName());
        assertEquals("https://gitee.com/gy2006/flow-test", t.getBase().getRepoUrl());

        assertEquals("1666376", t.getSender().getId());
        assertEquals("yang.guo", t.getSender().getName());
        assertEquals("gy2006", t.getSender().getUsername());
        assertEquals("benqyang_2006@hotmail.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        assertEquals("feature/222", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_pr_merge_event() {
        InputStream stream = load("gitee/webhook_pr_merge.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.PR, stream);

        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertNotNull(t);
        assertEquals(GitTrigger.GitEvent.PR_MERGED, t.getEvent());
        assertEquals(GitSource.GITEE, t.getSource());
        assertEquals(Boolean.TRUE, t.getMerged());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
