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

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class RoleDaoTest extends TestBase {

    @Autowired
    private RoleDao roleDao;

    @Test
    public void should_create_role_success() {
        Role role = new Role();
        role.setName("test");
        role.setDescription("test desc");
        roleDao.save(role);

        Assert.assertEquals("test", role.getName());
        Assert.assertEquals("test desc", role.getDescription());
        Assert.assertEquals(1, roleDao.list().size());
    }

    @Test
    public void should_update_role_success() {
        Role role = new Role();
        role.setName("test");
        role.setDescription("test desc");
        Role role1 = roleDao.save(role);
        role1.setName("test1");
        roleDao.update(role1);

        Assert.assertEquals("test1", role1.getName());
    }

    @Test
    public void should_delete_role_success() {
        Role role = new Role();
        role.setName("test");
        role.setDescription("test desc");
        roleDao.save(role);

        roleDao.delete(role);
        Assert.assertEquals(0, roleDao.list().size());
    }
}
