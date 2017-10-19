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
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public class GroupSystemInfo extends SystemInfo {

    private final static int DEFAULT_GROUP_NUM = 5;

    public GroupSystemInfo(Type type) {
        super(type);
    }

    public GroupSystemInfo(Status status, Type type) {
        super(status, type);
    }

    @Expose
    private Map<String, Map<String, String>> info = new HashMap<>(DEFAULT_GROUP_NUM);

    public Map<String, Map<String, String>> getInfo() {
        return info;
    }

    public void setInfo(Map<String, Map<String, String>> info) {
        this.info = info;
    }

    public void put(GroupName key, Map<String, String> content) {
        info.put(key.name(), content);
    }

    public Map<String, String> get(GroupName key) {
        return info.get(key.name());
    }

    public int size() {
        return info.size();
    }

}
