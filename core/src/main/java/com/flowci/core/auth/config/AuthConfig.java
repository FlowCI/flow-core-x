/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.auth.config;

import com.flowci.core.agent.domain.AgentAction;
import com.flowci.core.agent.domain.AgentHostAction;
import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.config.domain.ConfigAction;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.git.controller.GitActions;
import com.flowci.core.job.domain.JobAction;
import com.flowci.core.trigger.domain.TriggerActions;
import com.flowci.core.secret.controller.SecretActions;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.domain.UserAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Configuration
public class AuthConfig {

    @Bean
    public PermissionMap actionMap() {
        PermissionMap permissionMap = new PermissionMap();

        // admin
        permissionMap.add(User.Role.Admin, FlowAction.ALL);
        permissionMap.add(User.Role.Admin, JobAction.ALL);
        permissionMap.add(User.Role.Admin, SecretActions.ALL);
        permissionMap.add(User.Role.Admin, AgentAction.ALL);
        permissionMap.add(User.Role.Admin, AgentHostAction.ALL);
        permissionMap.add(User.Role.Admin, UserAction.ALL);
        permissionMap.add(User.Role.Admin, ConfigAction.ALL);
        permissionMap.add(User.Role.Admin, TriggerActions.ALL);
        permissionMap.add(User.Role.Admin, GitActions.ALL);
        permissionMap.add(User.Role.Admin, Settings.Action.ALL);

        // developer
        permissionMap.add(User.Role.Developer,
                FlowAction.GET, FlowAction.LIST, FlowAction.LIST_BRANCH, FlowAction.GET, FlowAction.GET_YML, FlowAction.LIST_USER);
        permissionMap.add(User.Role.Developer, JobAction.ALL);
        permissionMap.add(User.Role.Developer, SecretActions.LIST_NAME);
        permissionMap.add(User.Role.Developer, AgentAction.GET, AgentAction.LIST);
        permissionMap.add(User.Role.Developer, AgentHostAction.GET, AgentHostAction.LIST);
        permissionMap.add(User.Role.Developer, UserAction.CHANGE_PASSWORD, UserAction.UPDATE_AVATAR);
        permissionMap.add(User.Role.Developer, Settings.Action.GET);

        return permissionMap;
    }
}
