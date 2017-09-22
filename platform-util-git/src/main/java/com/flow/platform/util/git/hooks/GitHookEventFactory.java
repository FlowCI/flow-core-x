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

import static com.flow.platform.util.git.model.GitEventType.MR;
import static com.flow.platform.util.git.model.GitEventType.PUSH;
import static com.flow.platform.util.git.model.GitEventType.TAG;
import static com.flow.platform.util.git.model.GitSource.GITHUB;
import static com.flow.platform.util.git.model.GitSource.GITLAB;

import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: it could handle git webhook by plugin with interface
 *
 * @author yang
 */
public class GitHookEventFactory {

    /**
     * Key as header of hooks, value is event type and related hook event adaptor
     */
    private final static Map<String, Map<String, GitHookEventAdapter>> adaptors = new HashMap<>();

    static {
        // init GitLab hook data adaptor
        Map<String, GitHookEventAdapter> gitLabAdaptors = new HashMap<>(3);
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_MR, new GitLabEvents.MergeRequestAdaptor(GITLAB, MR));
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_PUSH, new GitLabEvents.PushAndTagAdapter(GITLAB, PUSH));
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_TAG, new GitLabEvents.PushAndTagAdapter(GITLAB, TAG));
        adaptors.put(GitLabEvents.Hooks.HEADER, gitLabAdaptors);

        // init GitHub hook data adaptor
        Map<String, GitHookEventAdapter> gitHubAdaptors = new HashMap<>(3);
        gitHubAdaptors.put(GitHubEvents.Hooks.EVENT_TYPE_PUSH, new GitHubEvents.PushAndTagAdapter(GITHUB, PUSH));
        gitHubAdaptors.put(GitHubEvents.Hooks.EVENT_TYPE_TAG, new GitHubEvents.PushAndTagAdapter(GITHUB, TAG));
        gitHubAdaptors.put(GitHubEvents.Hooks.EVENT_TYPE_MR, new GitHubEvents.MergeRequestAdapter(GITHUB, MR));
        adaptors.put(GitHubEvents.Hooks.HEADER, gitHubAdaptors);
    }

    /**
     * Create git hook event object from request header and body
     *
     * @throws GitException if cannot find matched adaptor
     */
    public static GitEvent build(Map<String, String> header, String json) throws GitException {
        GitHookEventAdapter matchedAdaptor = null;

        // looking for gitlab event adaptors
        String gitlabEventType = header.get(GitLabEvents.Hooks.HEADER);
        if (gitlabEventType != null) {
            matchedAdaptor = adaptors.get(GitLabEvents.Hooks.HEADER).get(gitlabEventType);
        }

        // looking for github event adaptors
        String githubEventType = header.get(GitHubEvents.Hooks.HEADER);
        if (githubEventType != null) {
            matchedAdaptor = adaptors.get(GitHubEvents.Hooks.HEADER).get(githubEventType);
        }

        if (matchedAdaptor != null) {
            return matchedAdaptor.convert(json);
        }

        throw new GitException("Illegal web hook request", null);
    }
}
