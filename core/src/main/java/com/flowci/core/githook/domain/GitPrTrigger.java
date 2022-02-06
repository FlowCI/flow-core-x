/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.githook.domain;

import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.flowci.core.common.domain.Variables.Git.*;

/**
 * @author yang
 */
@Getter
@Setter
public final class GitPrTrigger extends GitTrigger {

    private String title;

    private String body;

    private String url;

    private String number;

    private String time;

    private String numOfCommits;

    private String numOfFileChanges;

    private GitUser sender;

    private Boolean merged;

    private Source head; // from

    private Source base; // to

    @Override
    public StringVars toVariableMap() {
        StringVars map = super.toVariableMap();
        map.put(PR_AUTHOR, sender.getEmail());

        map.put(PR_TITLE, title);
        map.put(PR_MESSAGE, body);
        map.put(PR_URL, url);
        map.put(PR_TIME, time);
        map.put(PR_NUMBER, number);
        map.put(PR_IS_MERGED, String.valueOf(merged));

        map.put(PR_HEAD_REPO_NAME, head.repoName);
        map.put(PR_HEAD_REPO_BRANCH, head.ref);
        map.put(PR_HEAD_REPO_COMMIT, head.commit);

        map.put(PR_BASE_REPO_NAME, base.repoName);
        map.put(PR_BASE_REPO_BRANCH, base.ref);
        map.put(PR_BASE_REPO_COMMIT, base.commit);

        map.put(BRANCH, merged ? base.ref : head.ref);
        return map;
    }

    @Override
    public boolean isSkip() {
        if (!StringHelper.hasValue(title)) {
            return false;
        }
        return title.contains(SkipMessage);
    }

    @Override
    public String getId() {
        return buildId(getSource().name(), getEvent().name(), getNumber());
    }

    @Getter
    @Setter
    public static class Source {

        private String ref;

        private String commit;

        private String repoName;

        private String repoUrl;
    }
}
