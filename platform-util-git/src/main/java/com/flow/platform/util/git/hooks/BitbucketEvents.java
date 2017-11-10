/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.util.git.hooks;

import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventAuthor;
import com.flow.platform.util.git.model.GitEventCommit;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yh@firim
 */
public class BitbucketEvents {

    public static class Hooks {

        public final static String HEADER = "x-event-key";

        public final static String EVENT_TYPE_PUSH = "repo:push";

        public final static String EVENT_TYPE_PR_CREATED = "pullrequest:created";

        public final static String EVENT_TYPE_PR_UPDATED = "pullrequest:updated";

        public final static String EVENT_TYPE_PR_MERGERED = "pullrequest:fulfilled";
    }


    private class UserInfo {

        private String username;

        private String type;

        @SerializedName("display_name")
        private String displayName;

        private String uuid;
    }

    private class Repository {

        private String scm;

        private String name;

        @SerializedName("full_name")
        private String fullName;

        private UserInfo owner;

        @SerializedName(value = "is_private")
        private Boolean isPrivate;

        private String uuid;

    }

    private class AuthorHelper {

        private String raw;
        private String type;
        private UserInfo user;
    }

    private class HrefHelper {

        @SerializedName(value = "href")
        private String value;
    }


    private class CompareHelper {

        private HrefHelper commits;

        private HrefHelper html;

        private HrefHelper diff;
    }

    private class CommitHelper {

        private String hash;
        private AuthorHelper author;
        private String date;
        private String message;
        private String type;
        @SerializedName(value = "links")
        private CompareHelper diff;
    }


    public static class PushAndTagAdapter extends GitHookEventAdapter {

        private enum PushType {
            BRANCH,
            TAG
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        private class RootHelper {

            @SerializedName(value = "actor")
            private UserInfo pusher;

            private Repository repository;

            @SerializedName(value = "push")
            private AttrHelper attrs;
        }

        private class AttrHelper {

            private List<ChangeHelper> changes;
        }

        private class ChangeHelper {

            private Boolean forced;

            private Boolean truncated;

            private Boolean created;

            private Boolean closed;

            @SerializedName(value = "old")
            private PushHelper beforePush;

            @SerializedName(value = "new")
            private PushHelper afterPush;

            private List<CommitHelper> commits;

            @SerializedName(value = "links")
            private CompareHelper diff;
        }

        private class PushHelper {

            private String type;

            @SerializedName(value = "name")
            private String branchName;

            private CommitHelper target;

            private String message;
        }


        @Override
        public GitEvent convert(String json) throws GitException {
            RootHelper rootHelper = GSON.fromJson(json, RootHelper.class);
            GitPushTagEvent event;
            ChangeHelper changeHelper = rootHelper.attrs.changes.get(0);

            // push
            if (changeHelper.afterPush.type.toUpperCase().equals(PushType.BRANCH.toString())) {
                event = new GitPushTagEvent(GitSource.BITBUCKET, GitEventType.PUSH);
                event.setAfter(changeHelper.afterPush.target.hash);
                convertBranch(changeHelper, event);
                // tag
            } else if (changeHelper.afterPush.type.toUpperCase().equals(PushType.TAG.toString())) {
                event = new GitPushTagEvent(GitSource.BITBUCKET, GitEventType.TAG);
                convertTag(changeHelper, event);
            } else {
                return null;
            }

            event.setUsername(rootHelper.pusher.username);

            // can't get user email
            event.setUserEmail(rootHelper.pusher.username);
            event.setUserId(rootHelper.pusher.uuid);
            event.setHeadCommitUrl(changeHelper.afterPush.target.diff.html.value);

            return event;
        }

        private GitEvent convertBranch(ChangeHelper changeHelper, GitPushTagEvent event) {
            event.setRef("refs/heads/" + changeHelper.afterPush.branchName);
            event.setBefore(changeHelper.beforePush.target.hash);
            event.setMessage(changeHelper.afterPush.target.message);

            // set compare value
            event.setCompareUrl(changeHelper.diff.html.value);

            // set compare id
            final String compareId = GitPushTagEvent.buildCompareId(event);
            event.setCompareId(compareId);

            event.setCommits(new ArrayList<>(changeHelper.commits.size()));

            for (CommitHelper commit : changeHelper.commits) {
                GitEventCommit eventCommit = new GitEventCommit();
                eventCommit.setId(commit.hash);
                eventCommit.setMessage(commit.message);
                eventCommit.setTimestamp(commit.date);

                // set commit author
                GitEventAuthor author = new GitEventAuthor();
                author.setId(commit.author.user.uuid);
                author.setName(commit.author.user.username);
                eventCommit.setAuthor(author);
                eventCommit.setUrl(commit.diff.html.value);
                event.getCommits().add(eventCommit);
            }

            return event;
        }

        private GitEvent convertTag(ChangeHelper changeHelper, GitPushTagEvent event) {
            event.setRef("refs/tags/" + changeHelper.afterPush.branchName);
            event.setCommits(new ArrayList<>(0));
            event.setMessage(changeHelper.afterPush.message);
            event.setAfter(changeHelper.afterPush.target.hash);
            return event;
        }
    }


    public static class PullRequestAdapter extends GitHookEventAdapter {

        private class RootHelper {

            @SerializedName(value = "pullrequest")
            private PullRequestHelper pullRequest;

            @SerializedName(value = "actor")
            private UserInfo creator;

            private Repository repository;
        }

        private class PullRequestHelper {

            private String type;

            private String description;

            private String title;

            @SerializedName(value = "close_source_branch")
            private Boolean closeSourceBranch;

            private BranchHelper destination;

            private BranchHelper source;

            private UserInfo author;

            private String state;

            @SerializedName(value = "links")
            private CompareHelper diff;

        }

        private class BranchAttrHelper {

            private String name;
        }

        private class BranchHelper {

            private CommitHelper commit;
            private BranchAttrHelper branch;
            private Repository repository;
        }

        public final static String STATE_OPEN = "OPEN";

        public final static String STATE_CLOSE = "MERGED";

        public PullRequestAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {

            RootHelper rootHelper = GSON.fromJson(json, RootHelper.class);

            GitPullRequestEvent event = new GitPullRequestEvent(gitSource, eventType);

            if (Objects.equals(rootHelper.pullRequest.state, STATE_OPEN)) {
                event.setAction(STATE_OPEN);
                event.setSubmitter(rootHelper.creator.username);
            }

            if (Objects.equals(rootHelper.pullRequest.state, STATE_CLOSE)) {
                event.setAction(STATE_CLOSE);
                event.setSubmitter(rootHelper.creator.username);
            }

            event.setDescription(rootHelper.pullRequest.description);
            event.setTitle(rootHelper.pullRequest.title);
            event.setUrl(rootHelper.pullRequest.diff.html.value);
            event.setSource(new GitPullRequestInfo());
            event.setTarget(new GitPullRequestInfo());

            //can't get user email
            event.setUserEmail(rootHelper.creator.username);

            event.getSource().setBranch(rootHelper.pullRequest.source.branch.name);
            event.getSource().setSha(rootHelper.pullRequest.source.commit.hash);
            event.getSource().setProjectName(rootHelper.pullRequest.source.repository.fullName);

            event.getTarget().setBranch(rootHelper.pullRequest.destination.branch.name);
            event.getTarget().setSha(rootHelper.pullRequest.destination.commit.hash);
            event.getTarget().setProjectName(rootHelper.pullRequest.destination.repository.fullName);

            return event;
        }
    }
}
