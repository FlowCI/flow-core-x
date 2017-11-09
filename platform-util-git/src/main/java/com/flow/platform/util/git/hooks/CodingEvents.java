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
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * @author yang
 */
public class CodingEvents {

    public static class Hooks {

        public final static String HEADER = "x-coding-event";

        public final static String EVENT_TYPE_PUSH = "push";

        public final static String EVENT_TYPE_TAG = "push";

        public final static String EVENT_TYPE_PR = "pull_request";
    }

    public static class PushAndTagAdapter extends GitHookEventAdapter {

        private class PushHelper {

            private PushUserHelper user;

            private RepoHelper repository;
        }

        private class PushUserHelper {

            private String path;

            @SerializedName(value = "web_url")
            private String url;

            private String name;

        }

        private class RepoHelper {

            @SerializedName(value = "web_url")
            private String url;

            @SerializedName(value = "project_id")
            private String id;
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            PushHelper helper = GSON.fromJson(json, PushHelper.class);


            // for create tag event
            if (event.getRef().startsWith("refs/tags")) {
                event.setType(GitEventType.TAG);
            }

            // for branch push event
            else {
                event.setType(GitEventType.PUSH);
            }

            List<GitEventCommit> commits = event.getCommits();
            if (!commits.isEmpty()) {
                GitEventCommit latest = commits.get(0);
                event.setHeadCommitUrl(latest.getUrl());
                event.setMessage(latest.getMessage());

                GitEventAuthor author = latest.getAuthor();
                if (author != null) {
                    event.setUserId(author.getName());
                    event.setUsername(author.getName());
                    event.setUserEmail(author.getEmail());
                }
            }

            // set compare id
            final String compareId = GitPushTagEvent.buildCompareId(event);
            event.setCompareId(compareId);

            // set compare url
            final String compareUrl = helper.repository.url + "/git/compare/" + compareId;
            event.setCompareUrl(compareUrl);
            event.setGitSource(gitSource);
            return event;
        }
    }

}