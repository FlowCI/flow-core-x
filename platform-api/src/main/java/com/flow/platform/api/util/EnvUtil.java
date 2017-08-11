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

package com.flow.platform.api.util;

import com.flow.platform.api.domain.Node;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author yang
 */
public class EnvUtil {

    /**
     * Merget env variables of nodes
     *
     * @param from source node
     * @param to target node
     * @param overwrite is overwrite target node env if it is existed
     */
    public static void merge(Node from, Node to, boolean overwrite) {
        Map<String, String> sourceEnv = from.getEnvs();
        Map<String, String> targetEnv = to.getEnvs();

        for (Entry<String, String> entry : sourceEnv.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (targetEnv.containsKey(key)) {
                if (overwrite) {
                    targetEnv.put(key, value);
                }
                continue;
            }

            targetEnv.put(key, value);
        }
    }
}
