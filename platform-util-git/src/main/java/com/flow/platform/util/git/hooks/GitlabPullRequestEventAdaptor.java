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
import com.flow.platform.util.git.model.GitEventCommit;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import java.util.Map;

/**
 * @author yang
 */
public class GitlabPullRequestEventAdaptor extends PullRequestEventAdaptor {

    public GitlabPullRequestEventAdaptor(String jsonRaw) {
        super(jsonRaw);
    }

    @Override
    protected GitPullRequestEvent convert(Map raw) throws GitException {
        try {
            Map attrs = (Map) raw.get("object_attributes");

            GitPullRequestEvent prEvent = new GitPullRequestEvent();
            prEvent.setType(raw.get("object_kind").toString());
            prEvent.setTitle(attrs.get("title").toString());
            prEvent.setRequestId(toInteger(attrs.get("id").toString()));
            prEvent.setDescription(attrs.get("description").toString());
            prEvent.setTarget(new GitPullRequestInfo());
            prEvent.setSource(new GitPullRequestInfo());
            prEvent.setStatus(attrs.get("state").toString());
            prEvent.setMergeStatus(attrs.get("merge_status").toString());
            prEvent.setAction(attrs.get("action").toString());

            // set pr target info
            GitPullRequestInfo target = prEvent.getTarget();
            target.setBranch(attrs.get("target_branch").toString());
            target.setProjectId(toInteger(attrs.get("target_project_id").toString()));

            // set pr source info
            GitPullRequestInfo source = prEvent.getSource();
            source.setBranch(attrs.get("source_branch").toString());
            source.setProjectId(toInteger(attrs.get("source_project_id").toString()));

            // set commit to source
            String commitJson = GSON.toJson(attrs.get("last_commit"));
            source.setCommit(GSON.fromJson(commitJson, GitEventCommit.class));

            return prEvent;
        } catch (Throwable e) {
            throw new GitException("Illegal gitlab pull request data", e);
        }
    }
}
