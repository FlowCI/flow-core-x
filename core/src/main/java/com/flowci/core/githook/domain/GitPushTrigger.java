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
import lombok.ToString;

import static com.flowci.core.common.domain.Variables.Git.*;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(callSuper = true, of = {"ref", "message"})
public final class GitPushTrigger extends GitTrigger {

    private GitUser author;

    private String commitId;

    private String message;

    private String ref;

    private String time;

    private String commitUrl;

    private int numOfCommit;

    @Override
    public StringVars toVariableMap() {
        StringVars map = super.toVariableMap();

        map.put(GIT_BRANCH, ref);
        map.put(GIT_AUTHOR, author.getEmail());

        map.put(GIT_COMMIT_ID, commitId);
        map.put(GIT_COMMIT_MESSAGE, message);
        map.put(GIT_COMMIT_TIME, time);
        map.put(GIT_COMMIT_URL, commitUrl);
        map.put(GIT_COMMIT_NUM, Integer.toString(numOfCommit));

        // set empty string to PR variables
        for (String prVar : PR_VARS) {
            map.put(prVar, StringHelper.EMPTY);
        }

        return map;
    }

    @Override
    public boolean isSkip() {
        if (!StringHelper.hasValue(message)) {
            return false;
        }
        return message.contains(SkipMessage);
    }
}
