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

package com.flowci.core.user.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

public abstract class UserAction {

    public static final String LIST_ALL = "user_list_all";

    public static final String CHANGE_PASSWORD = "user_change_password";

    public static final String UPDATE_AVATAR = "user_avatar_update";

    public static final String CREATE_USER = "user_create";

    public static final String DELETE_USER = "user_delete";

    public static final String CHANGE_ROLE = "user_role_change";

    public static final List<String> ALL = ImmutableList.of(
            LIST_ALL,
            CHANGE_PASSWORD,
            UPDATE_AVATAR,
            CREATE_USER,
            DELETE_USER,
            CHANGE_ROLE
    );
}
