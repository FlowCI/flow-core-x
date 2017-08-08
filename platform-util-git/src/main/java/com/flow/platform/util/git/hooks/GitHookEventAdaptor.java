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
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author yang
 */
public abstract class GitHookEventAdaptor {

    /**
     * Default gson json serializer
     */
    final static Gson GSON = new Gson();

    protected GitSource gitSource;

    protected GitEventType eventType;

    public GitHookEventAdaptor(GitSource gitSource, GitEventType eventType) {
        this.gitSource = gitSource;
        this.eventType = eventType;
    }

    public abstract GitEvent convert(String json) throws GitException;

    static Map toMap(String json) {
        return GSON.fromJson(json, LinkedHashMap.class);
    }

    static Integer toInteger(String value) {
        return new Double(value).intValue();
    }
}
