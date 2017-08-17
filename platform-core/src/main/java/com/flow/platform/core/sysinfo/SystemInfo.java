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

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public class SystemInfo implements Serializable {

    private final static int DEFAULT_GROUP_NUM = 5;

    @Expose
    private Map<GroupName, Map<?, ?>> groupInfo = new HashMap<>(DEFAULT_GROUP_NUM);

    public Map<Object, Object> addGroup(GroupName name) {
        HashMap<Object, Object> value = new HashMap<>();
        groupInfo.putIfAbsent(name, value);
        return value;
    }

    public Map<?, ?> addGroup(GroupName name, int size) {
        HashMap<Object, Object> value = new HashMap<>(size);
        groupInfo.putIfAbsent(name, value);
        return value;
    }

    public void addGroup(GroupName name, Map<?, ?> data) {
        groupInfo.remove(name);
        groupInfo.put(name, data);
    }

    public int groupSize() {
        return groupInfo.size();
    }

    public Map<?, ?> getGroup(GroupName name) {
        return groupInfo.get(name);
    }
}
