/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.auth.domain;

import com.flowci.core.user.domain.User;

import java.util.*;

/**
 * Role vs Action map (not Admin)
 */
public class PermissionMap extends HashMap<User.Role, Set<String>> {

    public PermissionMap() {
        super();
    }

    public void add(User.Role role, String ...actions) {
        Set<String> set = get(role);

        if (Objects.isNull(set)) {
            set = new HashSet<>(20);
            put(role, set);
        }

        set.addAll(Arrays.asList(actions));
    }

    public boolean hasPermission(User.Role role, String action) {
        Set<String> actions = get(role);
        if (Objects.isNull(actions)) {
            return false;
        }

        return actions.contains(action);
    }
}
