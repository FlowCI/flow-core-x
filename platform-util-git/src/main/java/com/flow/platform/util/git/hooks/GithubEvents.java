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
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import java.util.Map;

/**
 * Adding GitHub web hook, should select 'Let me select individual events'
 * and select 'Push' and 'Pull request'
 *
 * @author yang
 */
public class GithubEvents {

    public static class Hooks {

        public final static String HEADER = "x-github-event";

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

            public Boolean getCreated() {
                return created;
            }

            public void setCreated(Boolean created) {
                this.created = created;
            }

            Map<String, String> getSender() {
                return sender;
            }

            public void setSender(Map<String, String> sender) {
                this.sender = sender;
            }
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            JsonHelper helper = GSON.fromJson(json, JsonHelper.class);

            // for create tag event
            if (event.getRef().startsWith("refs/tags") && helper.getCreated()) {
                event.setType(GitEventType.TAG);
            }

            // for branch push event
            else {
                event.setType(GitEventType.PUSH);
            }

            Map<String, String> sender = helper.getSender();
            if (sender != null) {
                event.setUserId(sender.get("id"));
                event.setUsername(sender.get("login"));
            }

            event.setGitSource(gitSource);
            return event;
        }
    }

    public static class MergeRequestAdapter extends GitHookEventAdapter {

        public MergeRequestAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            return null;
        }
    }
}
