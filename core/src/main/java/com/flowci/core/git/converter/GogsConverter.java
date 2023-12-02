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

package com.flowci.core.git.converter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.*;
import com.flowci.core.git.util.BranchHelper;
import com.flowci.common.exception.ArgumentException;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Component("gogsConverter")
public class GogsConverter extends TriggerConverter {

    public static final String Header = "x-gogs-event";

    public static final String Push = "push";

    public static final String Tag = "release";

    public static final String PR = "pull_request";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(Push, new EventConverter<>("Push", PushEvent.class))
                    .put(Tag, new EventConverter<>("Tag", ReleaseEvent.class))
                    .put(PR, new EventConverter<>("PR", PrEvent.class))
                    .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GOGS;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    // ======================================================
    //      Objects for GitHub
    // ======================================================

    private static class PushEvent implements GitTriggerable {

        public Repo repository;

        public String before;

        public String after;

        public String ref;

        public List<Commit> commits;

        public User pusher;

        @Override
        public GitTrigger toTrigger() {
            if (Objects.isNull(commits) || commits.isEmpty()) {
                throw new ArgumentException("No commits data on Gogs push event");
            }

            GitPushTrigger t = new GitPushTrigger();
            t.setSource(GitSource.GOGS);
            t.setEvent(GitTrigger.GitEvent.PUSH);
            t.setRef(BranchHelper.getBranchName(ref));
            t.setSender(pusher.toGitUser());
            t.setRepoId(repository.id);

            ObjectsHelper.ifNotNull(commits, vals -> {
                Commit commit = vals.get(0);
                t.setMessage(commit.message);
                t.setNumOfCommit(vals.size());
                t.setCommits(new ArrayList<>(vals.size()));

                for (var c : vals) {
                    t.getCommits().add(c.toGitCommit());
                }
            });
            return t;
        }
    }

    // Release event
    private static class ReleaseEvent implements GitTriggerable {

        public String action;

        public Release release;

        public Repo repository;

        @Override
        public GitTrigger toTrigger() {
            GitPushTrigger tag = new GitTagTrigger();
            tag.setEvent(GitTrigger.GitEvent.TAG);
            tag.setSource(GitSource.GOGS);
            tag.setRepoId(repository.id);
            tag.setRef(release.tagName);
            tag.setMessage(StringHelper.join(release.name, "\n", release.body).trim());
            tag.setSender(release.author.toGitUser());
            return tag;
        }
    }

    private static class PrEvent implements GitTriggerable {

        static final String ACTION_OPENED = "opened";

        static final String ACTION_CLOSED = "closed";

        public String action;

        @JsonAlias("pull_request")
        public PrBody prBody;

        public User sender;

        @Override
        public GitTrigger toTrigger() {
            GitPrTrigger trigger = new GitPrTrigger();
            setTriggerEvent(trigger);

            trigger.setSource(GitSource.GOGS);
            trigger.setNumber(prBody.number);
            trigger.setBody(prBody.body);
            trigger.setTitle(prBody.title);
            trigger.setUrl(prBody.url);
            trigger.setTime(prBody.mergedAt);
            trigger.setNumOfCommits(StringHelper.EMPTY);
            trigger.setNumOfFileChanges(StringHelper.EMPTY);
            trigger.setMerged(prBody.merged);

            GitPrTrigger.Source head = new GitPrTrigger.Source();
            head.setCommit(StringHelper.EMPTY);
            head.setRef(prBody.headBranch);
            head.setRepoName(prBody.head.fullName);
            head.setRepoUrl(prBody.head.url);
            trigger.setHead(head);

            GitPrTrigger.Source base = new GitPrTrigger.Source();
            base.setCommit(StringHelper.EMPTY);
            base.setRef(prBody.baseBranch);
            base.setRepoName(prBody.base.fullName);
            base.setRepoUrl(prBody.base.url);
            trigger.setBase(base);

            trigger.setSender(sender.toGitUser());

            if (!StringHelper.hasValue(trigger.getTime())) {
                trigger.setTime(StringHelper.EMPTY);
            }

            return trigger;
        }

        private void setTriggerEvent(GitPrTrigger trigger) {
            if (action.equals(ACTION_OPENED)) {
                trigger.setEvent(GitTrigger.GitEvent.PR_OPENED);
                return;
            }

            if (action.equals(ACTION_CLOSED) && prBody.merged) {
                trigger.setEvent(GitTrigger.GitEvent.PR_MERGED);
                return;
            }

            throw new ArgumentException("Cannot handle action {0} from pull request", action);
        }
    }

    private static class PrBody {

        public String id;

        public String number;

        public String title;

        public String body;

        public User user;

        @JsonAlias("html_url")
        public String url;

        @JsonAlias("head_repo")
        public Repo head;

        @JsonAlias("head_branch")
        public String headBranch;

        @JsonAlias("base_repo")
        public Repo base;

        @JsonAlias("base_branch")
        public String baseBranch;

        public boolean merged;

        @JsonAlias("merged_at")
        public String mergedAt;
    }

    private static class Repo {

        public String id;

        public String name;

        @JsonAlias("full_name")
        public String fullName;

        @JsonAlias("html_url")
        public String url;
    }

    private static class Release {

        public String id;

        @JsonAlias("tag_name")
        public String tagName;

        @JsonAlias("target_commitish")
        public String ref;

        // title
        public String name;

        public String body;

        @JsonAlias("created_at")
        public String createdAt;

        public User author;
    }

    private static class Commit {

        public String id;

        public String message;

        public String url;

        public User author;

        public String timestamp;

        public GitCommit toGitCommit() {
            return GitCommit.of(id, message, timestamp, url, author.toGitUser());
        }
    }

    private static class User {

        public String id;

        @JsonAlias("login")
        public String name;

        public String username;

        public String email;

        @JsonAlias("avatar_url")
        public String avatarUrl;

        GitUser toGitUser() {
            return new GitUser()
                    .setId(id)
                    .setName(name)
                    .setEmail(email)
                    .setUsername(username)
                    .setAvatarLink(avatarUrl);
        }
    }
}
