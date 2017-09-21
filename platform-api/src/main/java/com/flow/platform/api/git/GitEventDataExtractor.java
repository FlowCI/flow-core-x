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

import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPushTagEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Extract required GitEnv from git event
 *
 * @author yang
 */
public class GitEventDataExtractor {

    public static Map<String, String> extract(GitEvent event) {
        if (event instanceof GitPullRequestEvent) {
            GitPullRequestEvent pr = (GitPullRequestEvent) event;
            Map<String, String> info = new HashMap<>();
            info.put(GitEnvs.FLOW_GIT_EVENT_TYPE.name(), pr.getType().name());
            info.put(GitEnvs.FLOW_GIT_BRANCH.name(), simpleRef(pr.getTarget().getBranch()));
            info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), pr.getTitle());
            return info;
        }

        if (event instanceof GitPushTagEvent) {
            GitPushTagEvent pt = (GitPushTagEvent) event;
            Map<String, String> info = new HashMap<>();
            info.put(GitEnvs.FLOW_GIT_EVENT_TYPE.name(), pt.getType().name());
            info.put(GitEnvs.FLOW_GIT_BRANCH.name(), simpleRef(pt.getRef()));
            info.put(GitEnvs.FLOW_GIT_AUTHOR.name(), pt.getUsername());
            info.put(GitEnvs.FLOW_GIT_COMMIT_ID.name(), pt.getAfter());
            info.put(GitEnvs.FLOW_GIT_COMPARE_ID.name(), pt.getCompare());

            if (pt.getCommits().size() > 0) {
                info.put(GitEnvs.FLOW_GIT_CHANGELOG.name(), pt.getCommits().get(0).getMessage());
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
