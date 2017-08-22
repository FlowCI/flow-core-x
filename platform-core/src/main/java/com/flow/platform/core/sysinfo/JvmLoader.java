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

package com.flow.platform.core.sysinfo;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author yang
 */
public class JvmLoader implements SystemInfoLoader {

    public enum JvmGroup implements GroupName {
        OS,

        GENERAL,

        MEMORY
    }

    private final static Map<JvmGroup, Set<String>> GROUP_KEYS = new HashMap<>();

    static {
        GROUP_KEYS.put(JvmGroup.OS, Sets.newHashSet(
            "os.name",
            "os.version",
            "os.arch",
            "user.home",
            "user.language",
            "user.country"
        ));

        GROUP_KEYS.put(JvmGroup.GENERAL, Sets.newHashSet(
            "java.vm.version",
            "java.vm.vendor",
            "java.vm.name",
            "java.vm.specification.version",
            "java.home"
        ));
    }

    public SystemInfo load() {
        Properties properties = System.getProperties();
        SystemInfo jvm = new SystemInfo();
        jvm.setName(properties.getProperty("java.runtime.name"));
        jvm.setVersion(properties.getProperty("java.runtime.version"));

        jvm.put(JvmGroup.OS, buildFromProperties(JvmGroup.OS, properties));
        jvm.put(JvmGroup.GENERAL, buildFromProperties(JvmGroup.GENERAL, properties));

        // memory info
        Runtime runtime = Runtime.getRuntime();
        HashMap<String, String> memory = new HashMap<>();
        memory.put("java.vm.memory.free", Long.toString(runtime.freeMemory()));
        memory.put("java.vm.memory.max", Long.toString(runtime.maxMemory()));
        memory.put("java.vm.memory.total", Long.toString(runtime.totalMemory()));
        jvm.put(JvmGroup.MEMORY, memory);

        return jvm;
    }

    private Map<String, String> buildFromProperties(JvmGroup group, Properties properties) {
        Set<String> keys = GROUP_KEYS.get(group);
        HashMap<String, String> general = new HashMap<>(keys.size());
        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null) {
                general.put(key, value);
            }
        }
        return general;
    }

}
