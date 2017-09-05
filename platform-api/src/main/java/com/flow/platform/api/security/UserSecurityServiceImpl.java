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
package com.flow.platform.api.security;

import com.flow.platform.api.domain.Actions;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.Logger;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service
public class UserSecurityServiceImpl implements UserSecurityService {

    private final static Logger LOGGER = new Logger(UserSecurityServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ActionService actionService;

    @Override
    public boolean canAccess(String email, Action target) {
        User user = userService.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("Cannot find user by email: " + email);
        }

        List<Role> roles = roleService.list(user);

        for (Role role : roles) {
            List<Action> actions = permissionService.list(role);
            for (Action action : actions) {
                if (action.equals(target)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Action getAction(Actions actionName) {
        Action action = actionService.find(actionName.name());
        if (action == null) {
            throw new NotFoundException(String.format("Action '%s' is not registered", actionName));
        }
        return action;
    }
}


