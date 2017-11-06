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
package com.flow.platform.api.initializers;

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.util.StringUtil;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author lhl
 */

@Component
public class UserRoleInit extends Initializer {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ActionService actionService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ThreadLocal<User> currentUser;

    @Value(value = "${system.email}")
    private String email;

    @Value(value = "${system.username}")
    private String username;

    @Value(value = "${system.password}")
    private String password;

    @Override
    public void doStart() {
        // set current user for system default
        currentUser.set(new User("system@flow.ci", StringUtil.EMPTY, StringUtil.EMPTY));

        initSystemActions();

        initSystemRoles();

        initDefaultAdminUser();

        initSystemPermissions();
    }

    private void initSystemPermissions() {
        for (SysRole sysRole : SysRole.values()) {
            Role role = roleService.find(sysRole.name());

            for (Actions sysAction : sysRole.getActions()) {
                Action action = actionService.find(sysAction.name());

                try {
                    permissionService.assign(role, ImmutableList.of(action));
                } catch (FlowException ignore) {

                }
            }
        }
    }

    private void initDefaultAdminUser() {
        try {
            User admin = new User(email, username, password);
            userService.register(admin, ImmutableList.of(SysRole.ADMIN.name()), false, Collections.emptyList());
        } catch (FlowException ignore) {
            // ignore existed
        }
    }

    private void initSystemActions() {
        for (Actions systemAction : Actions.values()) {
            try {
                actionService.create(new Action(systemAction.name()));
            } catch (FlowException ignore) {
                // ignore duplication
            }
        }
    }

    private void initSystemRoles() {
        for (SysRole role : SysRole.values()) {
            try {
                roleService.create(role.name(), "System default role");
            } catch (FlowException ignore) {
                // ignore duplication
            }
        }
    }
}
