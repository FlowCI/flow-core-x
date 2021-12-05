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

package com.flowci.core.githook.converter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.githook.domain.*;
import com.flowci.exception.ArgumentException;
import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Log4j2
@Component("giteeConverter")
public class GiteeConverter extends TriggerConverter {

    public static final String Header = "X-Gitee-Event";

    public static final String HeaderForPing = "X-Gitee-Ping"; // X-Gitee-Ping: true

    public static final String Ping = "true";

    public static final String Push = "Push Hook";

    public static final String Tag = "Tag Push Hook";

    public static final String PR = "Merge Request Hook";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(Ping, new EventConverter<>("Ping", GiteeConverter.PingEvent.class))
                    .put(Push, new EventConverter<>("Push", GiteeConverter.PushOrTagEvent.class))
                    .put(Tag, new EventConverter<>("Tag", GiteeConverter.PushOrTagEvent.class))
                    .put(PR, new EventConverter<>("PR", GiteeConverter.PrEvent.class))
                    .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GITEE;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    private static class PingEvent implements GitTriggerable {

        @Override
        public GitPingTrigger toTrigger() {
            GitPingTrigger trigger = new GitPingTrigger();
            trigger.setSource(GitSource.GITEE);
            trigger.setEvent(GitTrigger.GitEvent.PING);
            return trigger;
        }
    }

    private static class PushOrTagEvent implements GitTriggerable {

        private static final String TagRefPrefix = "refs/tags";

        public String ref;

        @JsonProperty("head_commit")
        public Commit commit;

        @JsonProperty("total_commits_count")
        public int numOfCommit;

        public Author pusher;

        public Author sender;

        private GitTrigger.GitEvent getEvent() {
            return ref.startsWith(TagRefPrefix) ? GitTrigger.GitEvent.TAG : GitTrigger.GitEvent.PUSH;
        }

        @Override
        public GitTrigger toTrigger() {
            if (Objects.isNull(commit)) {
                throw new ArgumentException("No commits data on Gitee push or tag event");
            }

            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITEE);
            trigger.setEvent(getEvent());
//
//            trigger.setAuthor(pusher.toGitUser());
//            trigger.setCommitId(commit.id);
//            trigger.setMessage(commit.message);
//            trigger.setCommitUrl(commit.url);
//            trigger.setRef(BranchHelper.getBranchName(ref));
//            trigger.setTime(commit.timestamp);
//            trigger.setNumOfCommit(numOfCommit);
//
//            if (pusher.equals(sender)) {
//                trigger.getAuthor().setAvatarLink(sender.avatarUrl);
//            }

            return trigger;
        }
    }

    private static class PrEvent implements GitTriggerable {

        public static final String PrOpen = "open";

        public static final String PrMerged = "merge";

        public String action;

        @JsonProperty("pull_request")
        public PullRequest prBody;

        public Author sender;

        @Override
        public GitTrigger toTrigger() {
            GitPrTrigger trigger = new GitPrTrigger();
            trigger.setSource(GitSource.GITEE);
            trigger.setEvent(getEvent());

            trigger.setTitle(prBody.title);
            trigger.setBody(prBody.body);
            trigger.setTime(prBody.createdAt);
            trigger.setNumber(prBody.number);
            trigger.setUrl(prBody.url);
            trigger.setMerged(isMerged());
            trigger.setNumOfCommits(prBody.numOfCommits);
            trigger.setNumOfFileChanges(prBody.numOfFileChanges);
            trigger.setSender(sender.toGitUser());

            GitPrTrigger.Source head = new GitPrTrigger.Source();
            head.setCommit(prBody.head.sha);
            head.setRef(prBody.head.ref);
            head.setRepoName(prBody.head.repo.fullName);
            head.setRepoUrl(prBody.head.repo.url);
            trigger.setHead(head);

            GitPrTrigger.Source base = new GitPrTrigger.Source();
            base.setCommit(prBody.base.sha);
            base.setRef(prBody.base.ref);
            base.setRepoName(prBody.base.repo.fullName);
            base.setRepoUrl(prBody.base.repo.url);
            trigger.setBase(base);

            return trigger;
        }

        private boolean isMerged() {
            return PrMerged.equals(action);
        }

        private GitTrigger.GitEvent getEvent() {
            if (PrOpen.equals(action)) {
                return GitTrigger.GitEvent.PR_OPENED;
            }

            if (PrMerged.equals(action)) {
                return GitTrigger.GitEvent.PR_MERGED;
            }

            throw new ArgumentException("Cannot handle action {0} from pull request", action);
        }
    }

    private static class PullRequest {

        public String id;

        public String number;

        public String title;

        public String body;

        @JsonProperty("created_at")
        public String createdAt;

        @JsonProperty("html_url")
        public String url;

        @JsonProperty("commits")
        public String numOfCommits;

        @JsonProperty("changed_files")
        public String numOfFileChanges;

        public PrSource head;

        public PrSource base;
    }

    private static class PrSource {

        public String ref;

        public String sha;

        public PrRepo repo;
    }

    private static class PrRepo {

        public String id;

        @JsonProperty("full_name")
        public String fullName;

        @JsonProperty("html_url")
        public String url;
    }

    private static class Commit {

        public String id;

        public String message;

        public String timestamp;

        public String url;

        public Author author;
    }

    @EqualsAndHashCode(of = {"id"})
    private static class Author {

        public String id;

        public String name;

        public String email;

        public String username;

        @JsonProperty("avatar_url")
        public String avatarUrl;

        public GitUser toGitUser() {
            return new GitUser()
                    .setId(id)
                    .setEmail(email)
                    .setName(name)
                    .setAvatarLink(avatarUrl)
                    .setUsername(username);
        }
    }
}
