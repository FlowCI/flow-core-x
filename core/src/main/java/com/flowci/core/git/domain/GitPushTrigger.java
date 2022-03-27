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

package com.flowci.core.git.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flowci.core.common.helper.JacksonHelper;
import com.flowci.domain.StringVars;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

import static com.flowci.core.common.domain.Variables.Git.*;

/**
 * @author yang
 */
@Getter
@Setter
public class GitPushTrigger extends GitTrigger {

    private String repoId;

    private String ref;

    private String message;

    private int numOfCommit;

    private List<GitCommit> commits = Collections.emptyList();

    private GitUser sender;

    @Override
    public StringVars toVariableMap() {
        var map = super.toVariableMap();
        var commitListB64 = StringHelper.EMPTY;
        try {
            var json = JacksonHelper.Default.writeValueAsString(commits);
            commitListB64 = StringHelper.toBase64(json);
        } catch (JsonProcessingException e) {
            // ignore
        }

        map.put(BRANCH, ref);
        map.put(REPO_ID, repoId);
        map.put(PUSH_MESSAGE, message);
        map.put(PUSH_AUTHOR, sender.getEmail());
        map.put(PUSH_COMMIT_TOTAL, String.valueOf(numOfCommit));
        map.put(PUSH_COMMIT_LIST, commitListB64);
        return map;
    }

    @Override
    public boolean isSkip() {
        if (!ObjectsHelper.hasCollection(commits)) {
            return false;
        }

        GitCommit commit = commits.get(0);
        String message = commit.getMessage();
        return StringHelper.hasValue(message) && message.contains(SkipMessage);
    }

    @Override
    public String getId() {
        return buildId(getSource().name(), getEvent().name(), repoId, ref, commits.get(0).getId());
    }
}
