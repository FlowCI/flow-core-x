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

import com.flow.platform.api.service.GitService.Env;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPushTagEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public class GitEventDataExtractor {

    public static Map<String, String> extract(GitEvent event) {
        if (event instanceof GitPullRequestEvent) {
            GitPullRequestEvent pr = (GitPullRequestEvent) event;
            Map<String, String> info = new HashMap<>();
            info.put(Env.FLOW_GIT_EVENT_TYPE, pr.getType().name());
            info.put(Env.FLOW_GIT_BRANCH, simpleRef(pr.getTarget().getBranch()));
            info.put(Env.FLOW_GIT_CHANGELOG, pr.getTitle());
            return info;
        }

        if (event instanceof GitPushTagEvent) {
            GitPushTagEvent pt = (GitPushTagEvent) event;
            Map<String, String> info = new HashMap<>();
            info.put(Env.FLOW_GIT_EVENT_TYPE, pt.getType().name());
            info.put(Env.FLOW_GIT_BRANCH, simpleRef(pt.getRef()));

            if (pt.getCommits().size() > 0) {
                info.put(Env.FLOW_GIT_CHANGELOG, pt.getCommits().get(0).getMessage());
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
