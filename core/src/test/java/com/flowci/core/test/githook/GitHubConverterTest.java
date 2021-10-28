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

package com.flowci.core.test.githook;

import static com.flowci.core.githook.domain.Variables.GIT_BRANCH;
import static com.flowci.core.githook.domain.Variables.GIT_COMMIT_ID;
import static com.flowci.core.githook.domain.Variables.GIT_COMMIT_MESSAGE;
import static com.flowci.core.githook.domain.Variables.GIT_COMMIT_TIME;
import static com.flowci.core.githook.domain.Variables.GIT_COMMIT_URL;
import static com.flowci.core.githook.domain.Variables.GIT_EVENT;
import static com.flowci.core.githook.domain.Variables.GIT_SOURCE;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.githook.converter.GitHubConverter;
import com.flowci.core.githook.converter.TriggerConverter;
import com.flowci.core.githook.domain.GitPingTrigger;
import com.flowci.core.githook.domain.GitPrTrigger;
import com.flowci.core.githook.domain.GitPushTrigger;
import com.flowci.core.githook.domain.GitTrigger;
import com.flowci.core.githook.domain.GitTrigger.GitEvent;
import com.flowci.core.githook.domain.GitUser;
import com.flowci.core.githook.domain.Variables;
import com.flowci.domain.StringVars;
import java.io.InputStream;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class GitHubConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitHubConverter;

    @Test
    public void should_parse_ping_event() {
        InputStream stream = load("github/webhook_ping.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.Ping, stream);
        Assert.assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertTrue(trigger.getActive());
        Assert.assertTrue(trigger.getEvents().contains("pull_request"));
        Assert.assertTrue(trigger.getEvents().contains("push"));
        Assert.assertEquals("2019-08-23T20:35:35Z", trigger.getCreatedAt());
    }

    @Test
    public void should_parse_push_event() {
        InputStream stream = load("github/webhook_push.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        GitPushTrigger trigger = (GitPushTrigger) optional.get();

        // then: object properties should be parsed
        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitEvent.PUSH, trigger.getEvent());
        Assert.assertEquals(GitSource.GITHUB, trigger.getSource());

        Assert.assertEquals("40d0dd6e8e942643d794d7ed8d27610fb8729914", trigger.getCommitId());
        Assert.assertEquals("fdafadsf\n\ndfsdafad", trigger.getMessage());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/commit/40d0dd6e8e942643d794d7ed8d27610fb8729914",
            trigger.getCommitUrl());
        Assert.assertEquals("master", trigger.getRef());
        Assert.assertEquals("2017-08-08T11:19:05+08:00", trigger.getTime());

        GitUser author = trigger.getAuthor();
        Assert.assertEquals("yang-guo-2016", author.getName());
        Assert.assertEquals("gy@fir.im", author.getEmail());
        Assert.assertNull(author.getUsername());

        Assert.assertEquals(1, trigger.getNumOfCommit());

        // then: variables should be created
        StringVars variables = trigger.toVariableMap();
        Assert.assertNotNull(variables);

        Assert.assertEquals("PUSH", variables.get(GIT_EVENT));
        Assert.assertEquals("GITHUB", variables.get(GIT_SOURCE));

        Assert.assertEquals("master", variables.get(GIT_BRANCH));
        Assert.assertEquals("40d0dd6e8e942643d794d7ed8d27610fb8729914", variables.get(GIT_COMMIT_ID));
        Assert.assertEquals("fdafadsf\n\ndfsdafad", variables.get(GIT_COMMIT_MESSAGE));
        Assert.assertEquals("2017-08-08T11:19:05+08:00", variables.get(GIT_COMMIT_TIME));
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/commit/40d0dd6e8e942643d794d7ed8d27610fb8729914",
            variables.get(GIT_COMMIT_URL));
        Assert.assertEquals("gy@fir.im", variables.get(Variables.GIT_AUTHOR));
        Assert.assertEquals("1", variables.get(Variables.GIT_COMMIT_NUM));

    }

    @Test
    public void should_parse_tag_event() {
        InputStream stream = load("github/webhook_tag.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        GitPushTrigger trigger = (GitPushTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertEquals(GitEvent.TAG, trigger.getEvent());
        Assert.assertEquals(GitSource.GITHUB, trigger.getSource());

        Assert.assertEquals("26d1d0fa6ee44a8f4e02250d13e84bf02722f5e7", trigger.getCommitId());
        Assert.assertEquals("Update settings.gradle", trigger.getMessage());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/commit/26d1d0fa6ee44a8f4e02250d13e84bf02722f5e7",
            trigger.getCommitUrl());
        Assert.assertEquals("v1.6", trigger.getRef());
        Assert.assertEquals("2017-08-08T13:19:55+08:00", trigger.getTime());
        Assert.assertEquals(0, trigger.getNumOfCommit());

        GitUser author = trigger.getAuthor();
        Assert.assertEquals("yang-guo-2016", author.getName());
        Assert.assertEquals("gy@fir.im", author.getEmail());
        Assert.assertNull(author.getUsername());
    }

    @Test
    public void should_parse_pr_open_event() {
        InputStream stream = load("github/webhook_pr_open.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertEquals(GitEvent.PR_OPENED, trigger.getEvent());
        Assert.assertEquals(GitSource.GITHUB, trigger.getSource());

        Assert.assertEquals("2", trigger.getNumber());
        Assert.assertEquals("Update settings.gradle", trigger.getTitle());
        Assert.assertEquals("pr...", trigger.getBody());
        Assert.assertEquals("2017-08-08T03:07:15Z", trigger.getTime());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/pull/2", trigger.getUrl());
        Assert.assertEquals("1", trigger.getNumOfCommits());
        Assert.assertEquals("1", trigger.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, trigger.getMerged());

        Assert.assertEquals("8e7b8fb631ffcae6ae68338d0d16b381fdea4f31", trigger.getHead().getCommit());
        Assert.assertEquals("developer", trigger.getHead().getRef());
        Assert.assertEquals("yang-guo-2016/Test", trigger.getHead().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", trigger.getHead().getRepoUrl());

        Assert.assertEquals("ed6003bb96bd06cc75e38beb1176c5e9123ec607", trigger.getBase().getCommit());
        Assert.assertEquals("master", trigger.getBase().getRef());
        Assert.assertEquals("yang-guo-2016/Test", trigger.getBase().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", trigger.getBase().getRepoUrl());

        Assert.assertEquals("23307997", trigger.getSender().getId());
        Assert.assertEquals("yang-guo-2016", trigger.getSender().getUsername());
    }

    @Test
    public void should_parse_pr_close_event() {
        InputStream stream = load("github/webhook_pr_close.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger trigger = (GitPrTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertNotNull(trigger);
        Assert.assertEquals(GitEvent.PR_MERGED, trigger.getEvent());
        Assert.assertEquals(GitSource.GITHUB, trigger.getSource());

        Assert.assertEquals("7", trigger.getNumber());
        Assert.assertEquals("Update settings.gradle title", trigger.getTitle());
        Assert.assertEquals("hello desc", trigger.getBody());
        Assert.assertEquals("2017-08-08T06:26:35Z", trigger.getTime());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/pull/7", trigger.getUrl());
        Assert.assertEquals("1", trigger.getNumOfCommits());
        Assert.assertEquals("1", trigger.getNumOfFileChanges());
        Assert.assertEquals(Boolean.TRUE, trigger.getMerged());

        Assert.assertEquals("1d1de876084ef656e522f360b88c1e96acf6b806", trigger.getHead().getCommit());
        Assert.assertEquals("developer", trigger.getHead().getRef());
        Assert.assertEquals("yang-guo-2016/Test", trigger.getHead().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", trigger.getHead().getRepoUrl());

        Assert.assertEquals("4e4e3750cd468f245bd9f0f938c4b5f76e1bc5b0", trigger.getBase().getCommit());
        Assert.assertEquals("master", trigger.getBase().getRef());
        Assert.assertEquals("yang-guo-2016/Test", trigger.getBase().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", trigger.getBase().getRepoUrl());

        Assert.assertEquals("23307997", trigger.getSender().getId());
        Assert.assertEquals("yang-guo-2016", trigger.getSender().getUsername());
    }
}
