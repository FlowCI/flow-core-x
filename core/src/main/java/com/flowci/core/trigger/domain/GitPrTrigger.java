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

package com.flowci.core.trigger.domain;

import static com.flowci.core.trigger.domain.Variables.PR_BASE_REPO_BRANCH;
import static com.flowci.core.trigger.domain.Variables.PR_BASE_REPO_COMMIT;
import static com.flowci.core.trigger.domain.Variables.PR_BASE_REPO_NAME;
import static com.flowci.core.trigger.domain.Variables.PR_HEAD_REPO_BRANCH;
import static com.flowci.core.trigger.domain.Variables.PR_HEAD_REPO_COMMIT;
import static com.flowci.core.trigger.domain.Variables.PR_HEAD_REPO_NAME;
import static com.flowci.core.trigger.domain.Variables.PR_MESSAGE;
import static com.flowci.core.trigger.domain.Variables.PR_NUMBER;
import static com.flowci.core.trigger.domain.Variables.PR_TIME;
import static com.flowci.core.trigger.domain.Variables.PR_TITLE;
import static com.flowci.core.trigger.domain.Variables.PR_URL;

import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

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
        map.put(Variables.GIT_AUTHOR, sender.getUsername());

        map.put(PR_TITLE, title);
        map.put(PR_MESSAGE, body);
        map.put(PR_URL, url);
        map.put(PR_TIME, time);
        map.put(PR_NUMBER, number);

        map.put(PR_HEAD_REPO_NAME, head.repoName);
        map.put(PR_HEAD_REPO_BRANCH, head.ref);
        map.put(PR_HEAD_REPO_COMMIT, head.commit);

        map.put(PR_BASE_REPO_NAME, base.repoName);
        map.put(PR_BASE_REPO_BRANCH, base.ref);
        map.put(PR_BASE_REPO_COMMIT, base.commit);
        return map;
    }

    @Override
    public boolean isSkip() {
        if (!StringHelper.hasValue(title)) {
            return false;
        }
        return title.contains(SkipMessage);
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
