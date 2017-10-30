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

import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_EMAIL;
import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_NAME;
import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_PASSWORD;

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserRole;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.core.context.ContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lhl
 */

@Component
public class UserRoleInit extends Initializer {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private ThreadLocal<User> currentUser;

    @Override
    public void doStart() {

        // create sys user
        User user = userDao.get(DEFAULT_USER_EMAIL);
        if (user == null) {
            User sysUser = new User(DEFAULT_USER_EMAIL, DEFAULT_USER_NAME, DEFAULT_USER_PASSWORD);
            sysUser.setCreatedBy("system created");
            userDao.save(sysUser);
        }

        // create sys roleAdmin
        Role roleAdmin = roleDao.get(SysRole.ADMIN.name());
        if (roleAdmin == null) {
            Role sysRoleAdmin = new Role(SysRole.ADMIN.name(), "create default role for admin");
            sysRoleAdmin.setCreatedBy("system created");
            roleDao.save(sysRoleAdmin);
        }

        // create sys roleUser
        Role roleUser = roleDao.get(SysRole.USER.name());
        if (roleUser == null) {
            Role sysRoleUser = new Role(SysRole.USER.name(), "create default role for user");
            sysRoleUser.setCreatedBy("system created");
            roleDao.save(sysRoleUser);
        }

        // create sys user_role relation
        Role role = roleService.find(SysRole.ADMIN.name());
        User sysUser = userDao.get(DEFAULT_USER_EMAIL);
        UserRole userRole = userRoleDao.get(new UserRoleKey(role.getId(), sysUser.getEmail()));
        if (userRole == null) {
            currentUser.set(sysUser);
            roleService.assign(sysUser, role);
        }

    }
}
