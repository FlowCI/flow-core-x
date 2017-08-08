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
import com.flow.platform.util.git.model.GitSource;
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
        // init gitlab hook data adaptor
        Map<String, GitHookEventAdapter> gitlabAdaptors = new HashMap<>(3);
        gitlabAdaptors.put(
            GitlabEvents.Hooks.EVENT_TYPE_MR,
            new GitlabEvents.MergeRequestAdaptor(GitSource.GITLAB, GitEventType.MR));
        gitlabAdaptors.put(
            GitlabEvents.Hooks.EVENT_TYPE_PUSH,
            new GitlabEvents.PushAdapter(GitSource.GITLAB, GitEventType.PUSH));
        gitlabAdaptors
            .put(GitlabEvents.Hooks.EVENT_TYPE_TAG,
                new GitlabEvents.TagAdapter(GitSource.GITLAB, GitEventType.TAG));
        adaptors.put(GitlabEvents.Hooks.HEADER, gitlabAdaptors);

        // init github hook data adaptor
        Map<String, GitHookEventAdapter> githubAdaptors = new HashMap<>(3);
        githubAdaptors.put(
            GithubEvents.Hooks.EVENT_TYPE_PUSH,
            new GithubEvents.PushAndTagAdapter(GitSource.GITHUB, GitEventType.PUSH));
        githubAdaptors.put(
            GithubEvents.Hooks.EVENT_TYPE_MR,
            new GithubEvents.MergeRequestAdapter(GitSource.GITHUB, GitEventType.MR));
        adaptors.put(GithubEvents.Hooks.HEADER, githubAdaptors);
    }

    /**
     * Create git hook event object from request header and body
     *
     * @throws GitException if cannot find matched adaptor
     */
    public static GitEvent build(Map<String, String> header, String json) throws GitException {
        GitHookEventAdapter matchedAdaptor = null;

        // looking for gitlab event adaptors
        String gitlabEventType = header.get(GitlabEvents.Hooks.HEADER);
        if (gitlabEventType != null) {
            matchedAdaptor = adaptors.get(GitlabEvents.Hooks.HEADER).get(gitlabEventType);
        }

        // looking for github event adaptors
        String githubEventType = header.get(GithubEvents.Hooks.HEADER);
        if (githubEventType != null) {
            matchedAdaptor = adaptors.get(GithubEvents.Hooks.HEADER).get(githubEventType);
        }

        if (matchedAdaptor != null) {
            return matchedAdaptor.convert(json);
        }

        throw new GitException("Illegal web hook request", null);
    }
}
