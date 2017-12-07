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
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
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

            if (eventType == GitEventType.TAG) {
                event.setUserEmail(jsonHelper.user.name);
                event.setUsername(jsonHelper.user.name);
            }

            // return event
            return event;
        }
    }

}
