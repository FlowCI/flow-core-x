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
package com.flow.platform.api.test.service;

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.domain.user.UsersRoles;
import com.flow.platform.api.service.user.UsersRolesService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class UsersRolesServiceTest extends TestBase {

    @Autowired
    private UsersRolesService usersRolesService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    private User user;

    private Role role;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liuhailiang@126.com");
        userDao.save(user);

        role = new Role();
        role.setName("test_role");
        roleDao.save(role);
    }

    @Test
    public void should_create_user_roles() {
        UserRoleKey userRoleKey = new UserRoleKey(role.getId(), user.getEmail());
        UsersRoles usersRoles = new UsersRoles(userRoleKey);

        usersRolesService.create(usersRoles);

        Assert.assertEquals("liuhailiang@126.com", usersRoles.getEmail());
    }

    @Test
    public void should_delete_user_role() {
        UserRoleKey userRoleKey = new UserRoleKey(role.getId(), user.getEmail());
        UsersRoles usersRoles = new UsersRoles(userRoleKey);

        usersRolesService.create(usersRoles);

        usersRolesService.delete(userRoleKey);

        Assert.assertEquals(0, usersRolesService.listUsersRoles().size());
    }

    @Test
    public void should_list_users_roles(){
        UserRoleKey userRoleKey = new UserRoleKey(role.getId(), user.getEmail());
        UsersRoles usersRoles = new UsersRoles(userRoleKey);

        usersRolesService.create(usersRoles);
        Assert.assertEquals(1, usersRolesService.listUsersRoles().size());
    }

    @Test
    public void should_find_user_roles_by_email(){
        UserRoleKey userRoleKey = new UserRoleKey(role.getId(), user.getEmail());
        UsersRoles usersRoles = new UsersRoles(userRoleKey);

        usersRolesService.create(usersRoles);
        Assert.assertEquals(1, usersRolesService.listUsersRolesByEmail("liuhailiang@126.com").size());
    }

}
