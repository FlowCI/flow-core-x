/*
 * Copyright 2018 flow.ci
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
import com.flowci.core.git.converter.GitHubConverter;
import com.flowci.core.git.converter.TriggerConverter;
import com.flowci.core.git.domain.*;
import com.flowci.core.git.domain.GitTrigger.GitEvent;
import com.flowci.core.test.SpringScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class GitHubConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitHubConverter;

    @Test
    void should_parse_ping_event() {
        InputStream stream = load("github/webhook_ping.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.Ping, stream);
        assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        assertNotNull(trigger);

        assertTrue(trigger.getActive());
        assertTrue(trigger.getEvents().contains("pull_request"));
        assertTrue(trigger.getEvents().contains("push"));
        assertEquals("2019-08-23T20:35:35Z", trigger.getCreatedAt());
    }

    @Test
    void should_parse_push_event() {
        InputStream stream = load("github/webhook_push.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        assertTrue(optional.isPresent());

        // then: object properties should be parsed
        GitPushTrigger t = (GitPushTrigger) optional.get();
        assertEquals(GitEvent.PUSH, t.getEvent());
        assertEquals(GitSource.GITHUB, t.getSource());
        assertEquals("86284448", t.getRepoId());
        assertEquals(2, t.getNumOfCommit());
        assertEquals("second commit", t.getMessage());
        assertEquals("gy2006", t.getSender().getName());
        assertEquals("master", t.getRef());

        // then: verify commit data
        var commit1 = t.getCommits().get(0);
        assertEquals("01c3935c0e058eafb1a71da3b1da75dc35e69a9d", commit1.getId());
        assertEquals("first commit", commit1.getMessage());
        assertEquals("https://github.com/gy2006/ci-test/commit/01c3935c0e058eafb1a71da3b1da75dc35e69a9d", commit1.getUrl());
        assertEquals("2021-12-05T20:58:28+01:00", commit1.getTime());
        assertEquals("Yang Guo", commit1.getAuthor().getName());
        assertEquals("yang@Yangs-MacBook-Pro.local", commit1.getAuthor().getEmail());

        var commit2 = t.getCommits().get(1);
        assertEquals("410a0cda5875c3a1ede806e77c07be1382e2ebf3", commit2.getId());
        assertEquals("second commit", commit2.getMessage());
        assertEquals("https://github.com/gy2006/ci-test/commit/410a0cda5875c3a1ede806e77c07be1382e2ebf3", commit2.getUrl());
        assertEquals("2021-12-05T20:59:26+01:00", commit2.getTime());
        assertEquals("gy2006", commit2.getAuthor().getName());
        assertEquals("32008001@qq.com", commit2.getAuthor().getEmail());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_tag_event() {
        InputStream stream = load("github/webhook_tag.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        assertEquals(GitEvent.TAG, t.getEvent());
        assertEquals(GitSource.GITHUB, t.getSource());
        assertEquals("86284448", t.getRepoId());
        assertEquals("v2.1", t.getRef());
        assertEquals("second commit", t.getMessage());
        assertEquals("gy2006", t.getSender().getName());
        assertEquals("32008001@qq.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        assertEquals("v2.1", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_pr_open_event() {
        InputStream stream = load("github/webhook_pr_open.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertNotNull(t);

        assertEquals(GitEvent.PR_OPENED, t.getEvent());
        assertEquals(GitSource.GITHUB, t.getSource());

        assertEquals("2", t.getNumber());
        assertEquals("Update settings.gradle", t.getTitle());
        assertEquals("pr...", t.getBody());
        assertEquals("2017-08-08T03:07:15Z", t.getTime());
        assertEquals("https://github.com/yang-guo-2016/Test/pull/2", t.getUrl());
        assertEquals("1", t.getNumOfCommits());
        assertEquals("1", t.getNumOfFileChanges());
        assertEquals(Boolean.FALSE, t.getMerged());

        assertEquals("8e7b8fb631ffcae6ae68338d0d16b381fdea4f31", t.getHead().getCommit());
        assertEquals("developer", t.getHead().getRef());
        assertEquals("yang-guo-2016/Test", t.getHead().getRepoName());
        assertEquals("https://github.com/yang-guo-2016/Test", t.getHead().getRepoUrl());

        assertEquals("ed6003bb96bd06cc75e38beb1176c5e9123ec607", t.getBase().getCommit());
        assertEquals("master", t.getBase().getRef());
        assertEquals("yang-guo-2016/Test", t.getBase().getRepoName());
        assertEquals("https://github.com/yang-guo-2016/Test", t.getBase().getRepoUrl());

        assertEquals("23307997", t.getSender().getId());
        assertEquals("yang-guo-2016", t.getSender().getUsername());

        var vars = t.toVariableMap();
        assertEquals("developer", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_pr_close_event() {
        InputStream stream = load("github/webhook_pr_close.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        assertNotNull(t);

        assertNotNull(t);
        assertEquals(GitEvent.PR_MERGED, t.getEvent());
        assertEquals(GitSource.GITHUB, t.getSource());

        assertEquals("7", t.getNumber());
        assertEquals("Update settings.gradle title", t.getTitle());
        assertEquals("hello desc", t.getBody());
        assertEquals("2017-08-08T06:26:35Z", t.getTime());
        assertEquals("https://github.com/yang-guo-2016/Test/pull/7", t.getUrl());
        assertEquals("1", t.getNumOfCommits());
        assertEquals("1", t.getNumOfFileChanges());
        assertEquals(Boolean.TRUE, t.getMerged());

        assertEquals("1d1de876084ef656e522f360b88c1e96acf6b806", t.getHead().getCommit());
        assertEquals("developer", t.getHead().getRef());
        assertEquals("yang-guo-2016/Test", t.getHead().getRepoName());
        assertEquals("https://github.com/yang-guo-2016/Test", t.getHead().getRepoUrl());

        assertEquals("4e4e3750cd468f245bd9f0f938c4b5f76e1bc5b0", t.getBase().getCommit());
        assertEquals("master", t.getBase().getRef());
        assertEquals("yang-guo-2016/Test", t.getBase().getRepoName());
        assertEquals("https://github.com/yang-guo-2016/Test", t.getBase().getRepoUrl());

        assertEquals("23307997", t.getSender().getId());
        assertEquals("yang-guo-2016", t.getSender().getUsername());


        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
