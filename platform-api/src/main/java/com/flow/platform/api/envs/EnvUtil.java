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

package com.flow.platform.api.envs;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.domain.Cmd;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author yang
 */
public class EnvUtil {

    /**
     * Convert "\n" to "\\n" in env variable, therefore the env variable still keep \n
     * @param source source env variables
     * @param keys the keys needs to keep, or null for all variables
     */
    public static void keepNewlineForEnv(Map<String, String> source, Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            keys = source.keySet();
        }

        for (String key : keys) {
            String origin = source.get(key);
            source.put(key, origin.replace(Cmd.NEW_LINE, "\\" + Cmd.NEW_LINE));
        }
    }

    public static boolean hasRequired(Node node, Collection<String> requiredEnvSet) {
        for (String requiredKey : requiredEnvSet) {
            if (!node.getEnvs().containsKey(requiredKey)) {
                return false;
            }

            Object requiredValue = node.getEnvs().get(requiredKey);
            if (requiredValue == null || Strings.isNullOrEmpty(requiredValue.toString().trim())) {
                return false;
            }
        }

        return true;
    }

    public static boolean hasRequiredEnvKey(Node node, Collection<EnvKey> requiredEnvSet) {
        return hasRequired(node, toString(requiredEnvSet));
    }

    public static Collection<String> toString(Collection<EnvKey> keySet) {
        Set<String> strSet = new HashSet<>(keySet.size());
        for (EnvKey key : keySet) {
            strSet.add(key.name());
        }
        return strSet;
    }

    /**
     * Merget env variables of nodes
     *
     * @param from source node
     * @param to target node
     * @param overwrite is overwrite target node env if it is existed
     */
    public static void merge(Node from, Node to, boolean overwrite) {
        if (from == null || to == null) {
            return;
        }

        Map<String, String> sourceEnv = from.getEnvs();
        Map<String, String> targetEnv = to.getEnvs();
        merge(sourceEnv, targetEnv, overwrite);
    }

    public static void merge(Map<String, String> sourceEnv, Map<String, String> targetEnv, boolean overwrite) {
        if (sourceEnv == null || targetEnv == null) {
            return;
        }

        for (Entry<String, String> entry : sourceEnv.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            merge(key, value, targetEnv, overwrite);
        }
    }

    public static void merge(String key, String value, Map<String, String> targetEnv, boolean overwrite) {
        if (targetEnv.containsKey(key)) {
            if (overwrite) {
                targetEnv.put(key, value);
            }

            return;
        }

        targetEnv.put(key, value);
    }

    public static Map<String, String> build(String key, String value) {
        HashMap<String, String> single = new HashMap<>(1);
        single.put(key, value);
        return single;
    }

    public static Map<String, String> build(EnvKey key, EnvValue value) {
        return build(key.name(), value.value());
    }

    public static String get(Map<String, String> source, EnvKey key, String defaultValue) {
        String value = source.get(key.name());

        if (value == null) {
            return defaultValue;
        }

        return value;
    }
}
