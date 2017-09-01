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

package com.flow.platform.api.domain;

import com.flow.platform.api.domain.envs.EnvKey;
import com.flow.platform.api.domain.envs.EnvValue;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.gson.annotations.Expose;
import java.util.HashMap;
import java.util.Map;

/**
 * The object with environment variables map
 *
 * @author yang
 */
public abstract class EnvObject extends Jsonable {

    @YmlSerializer(required = false)
    @Expose
    protected Map<String, String> envs = new HashMap<>();

    public Map<String, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, String> envs) {
        this.envs = envs;
    }

    public String getEnv(EnvKey key) {
        return envs.get(key.name());
    }

    public String getEnv(String key) {
        return envs.get(key);
    }

    public void putEnv(EnvKey key, EnvValue value) {
        envs.put(key.name(), value.value());
    }

    public void putEnv(EnvKey key, String value) {
        envs.put(key.name(), value);
    }

    public void removeEnv(EnvKey key) {
        envs.remove(key.name());
    }
}
