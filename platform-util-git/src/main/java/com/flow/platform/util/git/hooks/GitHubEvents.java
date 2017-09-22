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
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * To adding GitHub web hook, should select 'Let me select individual events'
 * and then select 'Push' and 'Pull request'
 *
 * @author yang
 */
public class GitHubEvents {

    private final static String PULL_REQUEST_ACTION_CLOSED = "closed";

    public static class Hooks {

        public final static String HEADER = "x-github-event";

        public final static String EVENT_TYPE_PING = "ping";

        public final static String EVENT_TYPE_PUSH = "push";

        public final static String EVENT_TYPE_TAG = "push";

        public final static String EVENT_TYPE_MR = "pull_request";
    }

    /**
     * Branch push or create Tag event adaptor
     */
    public static class PushAndTagAdapter extends GitHookEventAdapter {

        private class JsonHelper {

            private Boolean created;

            private Map<String, String> sender;
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            JsonHelper helper = GSON.fromJson(json, JsonHelper.class);

            // for create tag event
            if (event.getRef().startsWith("refs/tags") && helper.created) {
                event.setType(GitEventType.TAG);
            }

            // for branch push event
            else {
                event.setType(GitEventType.PUSH);
            }

            Map<String, String> sender = helper.sender;
            if (sender != null) {
                event.setUserId(sender.get("id"));
                event.setUsername(sender.get("login"));
            }

            event.setCompareId(GitPushTagEvent.buildCompareId(event));
            event.setGitSource(gitSource);
            return event;
        }
    }

    public static class MergeRequestAdapter extends GitHookEventAdapter {

        private class MergeRequest {

            private String action;

            @SerializedName("pull_request")
            private PullRequest pullRequest;
        }

        private class PullRequest {

            private Integer id;

            private String state;

            private String title;

            @SerializedName("body")
            private String desc;

            @SerializedName("head")
            private PrInfo source;

            @SerializedName("base")
            private PrInfo target;
        }

        private class PrInfo {

            private String ref;

            private String sha;

            private PrRepo repo;
        }

        private class PrRepo {

            private Integer id;

            @SerializedName("full_name")
            private String name;
        }

        public MergeRequestAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            MergeRequest mr = GSON.fromJson(json, MergeRequest.class);
            PullRequest pullRequest = mr.pullRequest;

            GitPullRequestEvent event = new GitPullRequestEvent(gitSource, eventType);
            if (!mr.action.equals(PULL_REQUEST_ACTION_CLOSED)) {
                throw new GitException("Invalid GitHub pull request action", null);
            }

            event.setAction(mr.action);
            event.setRequestId(pullRequest.id);
            event.setDescription(pullRequest.desc);
            event.setTitle(pullRequest.title);
            event.setStatus(pullRequest.state);
            event.setSource(new GitPullRequestInfo());
            event.setTarget(new GitPullRequestInfo());

            // set source
            GitPullRequestInfo source = event.getSource();
            source.setProjectId(pullRequest.source.repo.id);
            source.setProjectName(pullRequest.source.repo.name);
            source.setBranch(pullRequest.source.ref);
            source.setSha(pullRequest.source.sha);

            // set target
            GitPullRequestInfo target = event.getTarget();
            target.setProjectId(pullRequest.target.repo.id);
            target.setProjectName(pullRequest.target.repo.name);
            target.setBranch(pullRequest.target.ref);
            target.setSha(pullRequest.target.sha);

            return event;
        }
    }
}
