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
import java.util.Map;

/**
 * To adding GitLab web hook,
 * should select 'Push events', 'Tag push events' and 'Merge Request events'
 *
 * @author yang
 */
public class GitlabEvents {

    public static class Hooks {

        public final static String HEADER = "x-gitlab-event";

        public final static String EVENT_TYPE_PUSH = "Push Hook";

        public final static String EVENT_TYPE_TAG = "Tag Push Hook";

        public final static String EVENT_TYPE_MR = "Merge Request Hook";
    }

    public static class PushAdapter extends GitHookEventAdapter {

        PushAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            event.setType(eventType);
            event.setGitSource(gitSource);
            return event;
        }
    }

    public static class TagAdapter extends GitHookEventAdapter {

        TagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            event.setType(eventType);
            event.setGitSource(gitSource);
            return event;
        }
    }

    public static class MergeRequestAdaptor extends GitHookEventAdapter {

        MergeRequestAdaptor(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            try {
                Map raw = toMap(json);
                Map attrs = (Map) raw.get("object_attributes");

                GitPullRequestEvent prEvent = new GitPullRequestEvent(gitSource, eventType);
                prEvent.setTitle(attrs.get("title").toString());
                prEvent.setRequestId(toInteger(attrs.get("id").toString()));
                prEvent.setDescription(attrs.get("description").toString());
                prEvent.setTarget(new GitPullRequestInfo());
                prEvent.setSource(new GitPullRequestInfo());
                prEvent.setStatus(attrs.get("state").toString());
                prEvent.setAction(attrs.get("action").toString());

                // set pr target info
                GitPullRequestInfo target = prEvent.getTarget();
                target.setBranch(attrs.get("target_branch").toString());
                target.setProjectId(toInteger(attrs.get("target_project_id").toString()));

                // set pr source info
                GitPullRequestInfo source = prEvent.getSource();
                source.setBranch(attrs.get("source_branch").toString());
                source.setProjectId(toInteger(attrs.get("source_project_id").toString()));

                Map lastCommit = (Map) attrs.get("last_commit");
                source.setSha(lastCommit.get("id").toString());

                return prEvent;
            } catch (Throwable e) {
                throw new GitException("Illegal gitlab pull request data", e);
            }
        }
    }
}
