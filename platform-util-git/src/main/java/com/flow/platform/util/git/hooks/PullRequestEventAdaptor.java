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
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public abstract class PullRequestEventAdaptor {

    protected final static Gson GSON = new Gson();

    private Map raw;

    public PullRequestEventAdaptor(String jsonRaw) {
        raw = GSON.fromJson(jsonRaw, HashMap.class);
    }

    public GitPullRequestEvent convert() {
        return convert(raw);
    }

    protected abstract GitPullRequestEvent convert(Map raw) throws GitException;

    public static Integer toInteger(String value) {
        return new Double(value).intValue();
    }
}
