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

package com.flowci.core.flow.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

public abstract class FlowAction {

    public static final String CREATE = "create_flow";

    public static final String CHECK_NAME = "check_name";

    public static final String UPDATE = "update_flow_settings";

    public static final String DELETE = "delete_flow";

    public static final String LIST = "list_flow";

    public static final String LIST_BY_CREDENTIAL = "list_flow_by_credential";

    public static final String GET = "get_flow";

    public static final String GET_YML = "get_yml";

    public static final String SET_YML = "set_yml";

    public static final String GIT_TEST = "git_test";

    public static final String LIST_BRANCH = "list_branch";

    public static final String SETUP_CREDENTIAL = "setup_credential";

    public static final String ADD_USER = "add_user_to_flow";

    public static final String REMOVE_USER = "remove_user_from_flow";

    public static final String LIST_USER = "list_user_of_flow";

    public static final String LIST_PLUGINS = "list_plugins";

    public static final String GROUP_READ = "group_read";
    public static final String GROUP_UPDATE = "group_operation";

    public static final List<String> ALL = ImmutableList.of(
            CREATE,
            CHECK_NAME,
            DELETE,
            UPDATE,
            LIST,
            LIST_BY_CREDENTIAL,
            GET,
            GET_YML,
            SET_YML,
            GIT_TEST,
            LIST_BRANCH,
            SETUP_CREDENTIAL,
            ADD_USER,
            REMOVE_USER,
            LIST_USER,
            LIST_PLUGINS,
            GROUP_READ,
            GROUP_UPDATE
    );
}
