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
import com.flow.platform.util.git.model.GitEventCommit;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent.State;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * @author yh@firim
 */
public class OschinaEvents {

    public static class Hooks {

        public final static String HEADER = "HTTP_X_GIT_OSCHINA_EVENT";

        public final static String EVENT_TYPE_PUSH = "Push Hook";

        public final static String EVENT_TYPE_TAG = "Tag Push Hook";

        public final static String EVENT_TYPE_PR = "Merge Request Hook";
    }


    public static class PushAndTagAdapter extends GitHookEventAdapter {

        private class JsonHelper {

            private Repository repository;

            private User user;
        }

        private class Repository {

            private String name;
            private String homepage;
            private String url;
            private String description;
        }

        private class User {

            private String name;
            private String username;
            private String url;
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            JsonHelper jsonHelper = GSON
                .fromJson(json, JsonHelper.class);

            event.setType(eventType);
            event.setGitSource(gitSource);

            // set head commit url
            final String headCommitUrl = jsonHelper.repository.homepage + "/commit/" + event.getAfter();
            event.setHeadCommitUrl(headCommitUrl);

            // set compare id
            final String compareId = GitPushTagEvent.buildCompareId(event);
            event.setCompareId(compareId);

            // set compare url
            final String compareUrl = jsonHelper.repository.homepage + "/compare/" + compareId;
            event.setCompareUrl(compareUrl);

            // set commit message
            if (!Objects.isNull(event.getCommits()) && !event.getCommits().isEmpty()) {
                GitEventCommit commit = event.getCommits().get(0);
                event.setMessage(commit.getMessage());
                event.setUserEmail(commit.getAuthor().getEmail());
            }

            // only tag set name and email
            if (eventType == GitEventType.TAG) {
                event.setUserEmail(jsonHelper.user.name);
                event.setUsername(jsonHelper.user.name);
            }

            // return event
            return event;
        }
    }

    public static class PullRequestAdaptor extends GitHookEventAdapter {

        private final static String STATE_OPEN = "opened";

        private final static String STATE_CLOSE = "merged";

        private class JsonHelper {

            @SerializedName(value = "iid")
            private Integer id;

            private String title;

            private String state;

            private String url;

            @SerializedName(value = "target_branch")
            private String targetBranch;

            @SerializedName(value = "source_branch")
            private String sourceBranch;

            private Author author;

            @SerializedName(value = "updated_by")
            private Author merger;

            private String action;

            @SerializedName(value = "target_repo")
            private RepoHelper targetRepo;

            @SerializedName(value = "source_repo")
            private RepoHelper sourceRepo;
        }

        private class Author {

            private String name;

            @SerializedName(value = "user_name")
            private String username;

            private String email;
        }

        private class RepoHelper {

            private ProjectHelper project;
        }

        private class ProjectHelper {

            @SerializedName(value = "name_with_namespace")
            private String name;
            private String path;
            private String url;
        }

        public PullRequestAdaptor(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPullRequestEvent prEvent = new GitPullRequestEvent(gitSource, eventType);
            JsonHelper jsonHelper = GSON.fromJson(json, JsonHelper.class);

            prEvent.setGitSource(GitSource.OSCHINA);
            prEvent.setAction(jsonHelper.action);
            prEvent.setDescription(jsonHelper.title);
            prEvent.setCompareUrl(jsonHelper.url);
            prEvent.setCompareId(jsonHelper.id.toString());
            prEvent.setRequestId(jsonHelper.id);
            prEvent.setUrl(jsonHelper.url);

            GitPullRequestInfo targetPullRequestInfo = new GitPullRequestInfo();
            targetPullRequestInfo.setBranch(jsonHelper.targetBranch);
            targetPullRequestInfo.setProjectName(jsonHelper.targetRepo.project.name);
            prEvent.setTarget(targetPullRequestInfo);

            GitPullRequestInfo sourcePullRequestInfo = new GitPullRequestInfo();
            sourcePullRequestInfo.setBranch(jsonHelper.sourceBranch);
            sourcePullRequestInfo.setProjectName(jsonHelper.sourceRepo.project.name);
            prEvent.setSource(sourcePullRequestInfo);

            if (Objects.equals(jsonHelper.state, STATE_OPEN)) {
                prEvent.setState(State.OPEN);
                prEvent.setSubmitter(jsonHelper.author.email);
            }

            if (Objects.equals(jsonHelper.state, STATE_CLOSE)) {
                prEvent.setState(State.CLOSE);

                // merger have no email
                prEvent.setMergedBy(jsonHelper.merger.name);
            }

            return prEvent;
        }
    }

}
