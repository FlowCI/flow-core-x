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

import com.flow.platform.api.domain.user.UserRole;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author lhl
 */
public class UserRoleDaoTest extends TestBase {

    @Test
    public void should_create_user_role() {
        UserRole usersRoles = new UserRole(new UserRoleKey(1, "liuhailiang@126.com"));
        userRoleDao.save(usersRoles);

        Assert.assertEquals("liuhailiang@126.com", usersRoles.getEmail());
        Assert.assertEquals(1, userRoleDao.list().size());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void should_raise_exception_if_create_duplicate_user_role() {
        UserRole usersRoles = new UserRole(new UserRoleKey(1, "liuhailiang@126.com"));
        userRoleDao.save(usersRoles);

        usersRoles = new UserRole(new UserRoleKey(1, "liuhailiang@126.com"));
        userRoleDao.save(usersRoles);
    }

    @Test
    public void should_find_user_role_by_email_and_delete() {
        final String email = "liuhailiang@126.com";

        // then: create two user-role for the same email
        UserRoleKey userRoleKey = new UserRoleKey(1, email);
        UserRole usersRoles = new UserRole(userRoleKey);
        userRoleDao.save(usersRoles);

        UserRoleKey userRoleKey1 = new UserRoleKey(2, email);
        UserRole usersRoles1 = new UserRole(userRoleKey1);
        userRoleDao.save(usersRoles1);

        Assert.assertEquals(2, userRoleDao.list(email).size());

        // then: delete user-role by email
        int numOfRows = userRoleDao.delete(email);
        Assert.assertEquals(2, numOfRows);
        Assert.assertEquals(0, userRoleDao.list(email).size());
    }

    @Test
    public void should_delete_user_role() {
        UserRoleKey userRoleKey = new UserRoleKey(1, "liuhailiang@126.com");
        UserRole usersRoles = new UserRole(userRoleKey);
        userRoleDao.save(usersRoles);

        userRoleDao.delete(usersRoles);
        Assert.assertEquals(0, userRoleDao.list().size());
    }

    @Test
    public void should_get_number_of_user_per_role() {
        final int roleId = 1;
        UserRoleKey userRoleKey = new UserRoleKey(roleId, "liuhailiang@126.com");
        UserRole usersRoles = new UserRole(userRoleKey);
        userRoleDao.save(usersRoles);

        userRoleKey = new UserRoleKey(roleId, "hello@126.com");
        usersRoles = new UserRole(userRoleKey);
        userRoleDao.save(usersRoles);

        Assert.assertEquals(2, userRoleDao.numOfUser(roleId).longValue());
    }

}
