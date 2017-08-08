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
import com.flow.platform.util.git.hooks.GitlabEvents.Hooks;
import com.flow.platform.util.git.hooks.GitlabEvents.PullRequestAdaptor;
import com.flow.platform.util.git.hooks.GitlabEvents.PushAdapter;
import com.flow.platform.util.git.hooks.GitlabEvents.TagAdapter;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public class GitHookEventFactory {

    /**
     * Key as header of hooks, value is event type and related hook event adaptor
     */
    private final static Map<String, Map<String, GitHookEventAdaptor>> adaptors = new HashMap<>();

    static {
        // init gitlab hook data adaptor
        Map<String, GitHookEventAdaptor> gitlabAdaptors = new HashMap<>(3);
        gitlabAdaptors.put(Hooks.EVENT_TYPE_PR, new PullRequestAdaptor(GitSource.GITLAB, GitEventType.PR));
        gitlabAdaptors.put(Hooks.EVENT_TYPE_PUSH, new PushAdapter(GitSource.GITLAB, GitEventType.PUSH));
        gitlabAdaptors.put(Hooks.EVENT_TYPE_TAG, new TagAdapter(GitSource.GITLAB, GitEventType.TAG));
        adaptors.put(Hooks.HEADER, gitlabAdaptors);
    }

    /**
     * Create git hook event object from request header and body
     *
     * @throws GitException if cannot find matched adaptor
     */
    public static GitEvent build(Map<String, String> header, String json) throws GitException {
        GitHookEventAdaptor matchedAdaptor = null;

        String gitlabEventType = header.get(Hooks.HEADER);
        if (gitlabEventType != null) {
            matchedAdaptor = adaptors.get(Hooks.HEADER).get(gitlabEventType);
        }

        if (matchedAdaptor != null) {
            return matchedAdaptor.convert(json);
        }

        throw new GitException("Illegal web hook request", null);
    }
}
