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

package com.flow.platform.api.git;

import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent.State;
import com.flow.platform.util.git.model.GitPushTagEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extract required GitEnv from git event
 *
 * @author yang
 */
public class GitEventEnvConverter {

    /**
     * Convert git event to job category
     * @param event source git event
     * @return job category
     */
    public static JobCategory convert(GitEventType event) {
        return JobCategory.valueOf(event.name());
    }

    public static Map<String, String> convert(GitCommit commit) {
        if (commit == null) {
            return Collections.emptyMap();
        }

        Map<String, String> info = new HashMap<>(3);
        info.put(GitEnvs.FLOW_GIT_COMMIT_ID.name(), commit.getId());
        info.put(GitEnvs.FLOW_GIT_AUTHOR.name(), commit.getAuthor());
        info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), commit.getMessage());
        return info;
    }

    public static Map<String, String> convert(GitEvent event) {
        if (event instanceof GitPullRequestEvent) {
            GitPullRequestEvent pr = (GitPullRequestEvent) event;
            Map<String, String> info = new HashMap<>(6);
            info.put(GitEnvs.FLOW_GIT_EVENT_TYPE.name(), pr.getType().name());
            info.put(GitEnvs.FLOW_GIT_EVENT_SOURCE.name(), pr.getGitSource().name());

            // the branch is on source for open pr
            if (pr.getState() == State.OPEN) {
                info.put(GitEnvs.FLOW_GIT_BRANCH.name(), simpleRef(pr.getSource().getBranch()));
                info.put(GitEnvs.FLOW_GIT_AUTHOR.name(), pr.getSubmitter());
            }

            // the branch is on target for close pr
            if (pr.getState() == State.CLOSE) {
                info.put(GitEnvs.FLOW_GIT_BRANCH.name(), simpleRef(pr.getTarget().getBranch()));
                info.put(GitEnvs.FLOW_GIT_AUTHOR.name(), pr.getMergedBy());
            }

            // set commit id and url from PR request id and pr url
            if (pr.getRequestId() != null && pr.getUrl() != null) {
                info.put(GitEnvs.FLOW_GIT_COMMIT_ID.name(), pr.getRequestId().toString());
                info.put(GitEnvs.FLOW_GIT_COMMIT_URL.name(), pr.getUrl());
            }

            info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), pr.getTitle());
            info.put(GitEnvs.FLOW_GIT_PR_URL.name(), pr.getUrl());
            return info;
        }

        if (event instanceof GitPushTagEvent) {
            GitPushTagEvent pt = (GitPushTagEvent) event;
            Map<String, String> info = new HashMap<>(10);
            info.put(GitEnvs.FLOW_GIT_EVENT_TYPE.name(), pt.getType().name());
            info.put(GitEnvs.FLOW_GIT_EVENT_SOURCE.name(), pt.getGitSource().name());
            info.put(GitEnvs.FLOW_GIT_BRANCH.name(), simpleRef(pt.getRef()));
            info.put(GitEnvs.FLOW_GIT_AUTHOR.name(), pt.getUsername());
            info.put(GitEnvs.FLOW_GIT_AUTHOR_EMAIL.name(), pt.getUserEmail());
            info.put(GitEnvs.FLOW_GIT_COMMIT_ID.name(), pt.getAfter());
            info.put(GitEnvs.FLOW_GIT_COMMIT_URL.name(), pt.getHeadCommitUrl());
            info.put(GitEnvs.FLOW_GIT_COMPARE_URL.name(), pt.getCompareUrl());
            info.put(GitEnvs.FLOW_GIT_COMPARE_ID.name(), pt.getCompareId());

            // TODO: multi change log
            if (pt.getCommits().size() > 0) {
                info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), pt.getCommits().get(0).getMessage());
            } else if (Objects.equals(pt.getType(), GitEventType.TAG)) {
                info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), pt.getMessage());
            }

            return info;
        }

        throw new IllegalParameterException("Git event type not supported");
    }

    /**
     * Simplify ref from 'ref/head/master' to 'master'
     */
    private static String simpleRef(String ref) {
        int slashIndex = ref.lastIndexOf('/');
        if (slashIndex == -1) {
            return ref;
        }
        return ref.substring(slashIndex + 1);
    }
}
