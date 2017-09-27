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

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.RoleName;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.core.context.ContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lhl
 */

@Component
public class UserRoleInit implements ContextEvent {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Override
    public void start(){

        // create sys user
        User user = userDao.get("admin@admin.com");
        if (user == null){
            User sysUser = new User("admin@admin.com", "admin", "123456");
            sysUser.setCreatedBy("system created");
            userDao.save(sysUser);
        }

        // create sys roleAdmin
        Role roleAdmin = roleDao.get(RoleName.ADMIN.getName());
        if (roleAdmin == null){
            Role sysRoleAdmin = new Role(RoleName.ADMIN.getName(), "create default role for admin");
            sysRoleAdmin.setCreatedBy("system created");
            roleDao.save(sysRoleAdmin);
        }

        // create sys roleUser
        Role roleUser = roleDao.get(RoleName.USER.getName());
        if (roleUser == null){
            Role sysRoleUser = new Role(RoleName.USER.getName(), "create default role for user");
            sysRoleUser.setCreatedBy("system created");
            roleDao.save(sysRoleUser);
        }

        // create sys user_role relation
//        Role role = roleService.find(RoleName.ADMIN.getName());
////        roleService.unAssign(user, role);
//        roleService.assign(user, role);
    }

    @Override
    public void stop(){

    }
}
