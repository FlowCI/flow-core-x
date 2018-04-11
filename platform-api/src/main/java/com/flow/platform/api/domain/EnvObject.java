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

import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.EnvValue;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.gson.annotations.Expose;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * The object with environment variables map
 *
 * @author yang
 */
@NoArgsConstructor
@AllArgsConstructor
public class EnvObject extends Jsonable {

    @YmlSerializer(required = false)
    @Expose
    protected Map<String, String> envs = new LinkedHashMap<>();

    public Map<String, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, String> envs) {
        this.envs = envs;
    }

    /**
     * Get env value by EnvKey
     */
    public String getEnv(EnvKey key) {
        return envs.get(key.name());
    }

    /**
     * Get env value by EnvKey
     * Return default value if env value is null
     */
    public String getEnv(EnvKey key, String defaultValue) {
        return getEnv(key.name(), defaultValue);
    }

    /**
     * Get env value by string key
     */
    public String getEnv(String key) {
        return envs.get(key);
    }

    /**
     * Get env value by string key
     * Return default value if env value is null
     */
    public String getEnv(String key, String defaultValue) {
        String value = envs.get(key);
        return value == null ? defaultValue : value;
    }

    public void putEnv(EnvKey key, EnvValue value) {
        envs.put(key.name(), value.value());
    }

    public void putEnv(EnvKey key, String value) {
        envs.put(key.name(), value);
    }

    public void putAll(Map<String, String> envs) {
        EnvUtil.merge(envs, this.envs, true);
    }

    public void removeEnv(EnvKey key) {
        envs.remove(key.name());
    }

    public void removeEnv(String key) {
        envs.remove(key);
    }
}
