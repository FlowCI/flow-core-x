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

import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class RoleServiceTest extends TestBase {

    @Autowired
    private RoleService roleService;

    @Test
    public void should_create_role() {
        roleService.create("test", "");
        Assert.assertEquals(1, roleService.list().size());
    }

    @Test
    public void should_update_role() {
        // when: update
        Role role = roleService.create("test", "");
        role.setName("test_update");
        roleService.update(role);

        // then
        Assert.assertNotNull(roleService.find("test_update"));
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_role_been_deleted() {
        // when:
        roleService.create("test", null);
        Assert.assertNotNull(roleService.find("test"));

        // then: raise exception if find role by name
        roleService.delete("test");
        roleService.find("test");
    }

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_if_user_been_assigned_to_role() {
        // given:
        final String roleName = "admin";
        final String email = "test@hello.com";

        User user = new User(email, "test", "12345");
        userDao.save(user);
        Assert.assertNotNull(userDao.get(user.getEmail()));

        Role role = roleService.create(roleName, "for test");
        Assert.assertNotNull(roleService.find(role.getName()));

        // when:
        roleService.assign(user, role);

        // then:
        roleService.delete(roleName);
    }

    @Test
    public void should_assign_user_to_role() {
        // given:
        final String roleName = "admin";
        final String email = "test@hello.com";

        User user = new User(email, "test", "12345");
        userDao.save(user);
        Assert.assertNotNull(userDao.get(user.getEmail()));

        Role role = roleService.create(roleName, "for test");
        Assert.assertNotNull(roleService.find(role.getName()));

        // when: assign user to role
        roleService.assign(user, role);

        // then:
        List<User> usersForRole = roleService.list(roleName);
        Assert.assertEquals(1, usersForRole.size());
        Assert.assertEquals(user, usersForRole.get(0));

        // when: un-assign user to role
        roleService.unAssign(user, role);

        // then:
        usersForRole = roleService.list(roleName);
        Assert.assertEquals(0, usersForRole.size());
    }
}
