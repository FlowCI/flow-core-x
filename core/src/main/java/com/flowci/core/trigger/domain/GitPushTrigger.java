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

import com.flowci.domain.StringVars;
import java.io.Serializable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

        map.put(Variables.GIT_BRANCH, ref);
        map.put(Variables.GIT_AUTHOR, author.getEmail());

        map.put(Variables.GIT_COMMIT_ID, commitId);
        map.put(Variables.GIT_COMMIT_MESSAGE, message);
        map.put(Variables.GIT_COMMIT_TIME, time);
        map.put(Variables.GIT_COMMIT_URL, commitUrl);
        map.put(Variables.GIT_COMMIT_NUM, Integer.toString(numOfCommit));
        return map;
    }
}
