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

package com.flowci.tree.yml;

import com.flowci.domain.StringVars;
import com.flowci.tree.Node;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author yang
 */
@Setter
@Getter
public abstract class YmlNode implements Serializable {

    public String name;

    public Map<String, String> envs = new LinkedHashMap<>();

    public abstract Node toNode(int index);

    StringVars getVariableMap() {
        StringVars variables = new StringVars(envs.size());
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            variables.put(entry.getKey(), entry.getValue());
        }
        return variables;
    }

    void setEnvs(StringVars variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            this.envs.put(entry.getKey(), entry.getValue());
        }
    }
}
