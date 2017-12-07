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

import static com.flow.platform.util.git.model.GitEventType.NONE;
import static com.flow.platform.util.git.model.GitEventType.PR;
import static com.flow.platform.util.git.model.GitEventType.PUSH;
import static com.flow.platform.util.git.model.GitEventType.TAG;
import static com.flow.platform.util.git.model.GitSource.BITBUCKET;
import static com.flow.platform.util.git.model.GitSource.CODING;
import static com.flow.platform.util.git.model.GitSource.GITHUB;
import static com.flow.platform.util.git.model.GitSource.GITLAB;
import static com.flow.platform.util.git.model.GitSource.OSCHINA;

import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.hooks.BitbucketEvents.Hooks;
import com.flow.platform.util.git.hooks.BitbucketEvents.PushAndTagAdapter;
import com.flow.platform.util.git.hooks.GitHubEvents.PullRequestAdapter;
import com.flow.platform.util.git.hooks.GitLabEvents.PullRequestAdaptor;
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
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_PR, new PullRequestAdaptor(GITLAB, PR));
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_PUSH, new GitLabEvents.PushAndTagAdapter(GITLAB, PUSH));
        gitLabAdaptors.put(GitLabEvents.Hooks.EVENT_TYPE_TAG, new GitLabEvents.PushAndTagAdapter(GITLAB, TAG));
        adaptors.put(GitLabEvents.Hooks.HEADER, gitLabAdaptors);

        // init GitHub hook data adaptor
        Map<String, GitHookEventAdapter> gitHubAdaptors = new HashMap<>(2);
        gitHubAdaptors.put(GitHubEvents.Hooks.EVENT_TYPE_PR, new PullRequestAdapter(GITHUB, PR));
        gitHubAdaptors.put(GitHubEvents.Hooks.EVENT_TYPE_PUSH_OR_TAG, new GitHubEvents.PushAndTagAdapter(GITHUB, NONE));
        adaptors.put(GitHubEvents.Hooks.HEADER, gitHubAdaptors);

        Map<String, GitHookEventAdapter> codingAdaptors = new HashMap<>(2);
        codingAdaptors.put(CodingEvents.Hooks.EVENT_TYPE_PR, new CodingEvents.PullRequestAdapter(CODING, PR));
        codingAdaptors.put(CodingEvents.Hooks.EVENT_TYPE_PUSH_OR_TAG, new CodingEvents.PushAndTagAdapter(CODING, NONE));
        adaptors.put(CodingEvents.Hooks.HEADER, codingAdaptors);

        Map<String, GitHookEventAdapter> bitbucketAdaptors = new HashMap<>(4);
        bitbucketAdaptors.put(Hooks.EVENT_TYPE_PUSH, new PushAndTagAdapter(BITBUCKET, PUSH));
        bitbucketAdaptors.put(Hooks.EVENT_TYPE_PR_MERGERED, new BitbucketEvents.PullRequestAdapter(BITBUCKET, PR));
        bitbucketAdaptors.put(Hooks.EVENT_TYPE_PR_CREATED, new BitbucketEvents.PullRequestAdapter(BITBUCKET, PR));
        bitbucketAdaptors.put(Hooks.EVENT_TYPE_PR_UPDATED, new BitbucketEvents.PullRequestAdapter(BITBUCKET, PR));
        adaptors.put(Hooks.HEADER, bitbucketAdaptors);

        Map<String, GitHookEventAdapter> oschinaAdaptors = new HashMap<>(3);
        oschinaAdaptors.put(OschinaEvents.Hooks.EVENT_TYPE_PUSH, new OschinaEvents.PushAndTagAdapter(OSCHINA, PUSH));
        oschinaAdaptors.put(OschinaEvents.Hooks.EVENT_TYPE_TAG, new OschinaEvents.PushAndTagAdapter(OSCHINA, TAG));
        oschinaAdaptors.put(OschinaEvents.Hooks.EVENT_TYPE_PR, new OschinaEvents.PullRequestAdaptor(OSCHINA, PR));
        adaptors.put(OschinaEvents.Hooks.HEADER, oschinaAdaptors);
    }

    /**
     * Create git hook event object from request header and body
     *
     * @throws GitException if cannot find matched adaptor
     */
    public static GitEvent build(Map<String, String> header, String json) throws GitException {
        GitHookEventAdapter matchedAdaptor = null;

        // looking for GitLab event adaptors
        String gitLabEventType = header.get(GitLabEvents.Hooks.HEADER);
        if (gitLabEventType != null) {
            matchedAdaptor = adaptors.get(GitLabEvents.Hooks.HEADER).get(gitLabEventType);
        }

        // looking for GitHub event adaptors
        String gitHubEventType = header.get(GitHubEvents.Hooks.HEADER);
        if (gitHubEventType != null) {
            matchedAdaptor = adaptors.get(GitHubEvents.Hooks.HEADER).get(gitHubEventType);
        }

        // looking for Coding event adaptors
        String codingEventType = header.get(CodingEvents.Hooks.HEADER);
        if (codingEventType != null) {
            matchedAdaptor = adaptors.get(CodingEvents.Hooks.HEADER).get(codingEventType);
        }

        // looking for Bitbucket event adaptors
        String bitbucketEventType = header.get(Hooks.HEADER);
        if (bitbucketEventType != null) {
            matchedAdaptor = adaptors.get(BitbucketEvents.Hooks.HEADER).get(bitbucketEventType);
        }

        // looking for Oschina event adaptors
        String oschinaEventType = header.get(OschinaEvents.Hooks.HEADER);
        if (oschinaEventType != null) {
            matchedAdaptor = adaptors.get(OschinaEvents.Hooks.HEADER).get(oschinaEventType);
        }

        if (matchedAdaptor != null) {
            return matchedAdaptor.convert(json);
        }

        throw new GitException("Illegal web hook request", null);
    }
}
