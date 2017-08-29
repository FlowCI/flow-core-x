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
package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.user.UsersRolesDao;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.domain.user.UsersRoles;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class UsersRolesDaoTest extends TestBase {

    @Autowired
    private UsersRolesDao usersRolesDao;

    @Test
    public void should_create_user_role() {
        UserRoleKey userRoleKey = new UserRoleKey(1, "liuhailiang@126.com");
        UsersRoles usersRoles = new UsersRoles(userRoleKey);
        usersRolesDao.save(usersRoles);

        Assert.assertEquals("liuhailiang@126.com", usersRoles.getEmail());
        Assert.assertEquals(1, usersRolesDao.list().size());
    }

    @Test
    public void should_find_users_roles_by_email() {
        UserRoleKey userRoleKey = new UserRoleKey(1, "liuhailiang@126.com");
        UsersRoles usersRoles = new UsersRoles(userRoleKey);
        usersRolesDao.save(usersRoles);

        UserRoleKey userRoleKey1 = new UserRoleKey(2, "liuhailiang@126.com");
        UsersRoles usersRoles1 = new UsersRoles(userRoleKey1);
        usersRolesDao.save(usersRoles1);

        Assert.assertEquals(2, usersRolesDao.list("liuhailiang@126.com").size());
    }

    @Test
    public void should_delete_users_roles() {
        UserRoleKey userRoleKey = new UserRoleKey(1, "liuhailiang@126.com");
        UsersRoles usersRoles = new UsersRoles(userRoleKey);
        usersRolesDao.save(usersRoles);

        usersRolesDao.delete(usersRoles);

        Assert.assertEquals(0, usersRolesDao.list().size());
    }

}
