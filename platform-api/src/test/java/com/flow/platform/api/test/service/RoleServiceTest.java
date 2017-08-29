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
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.test.TestBase;
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
    public void should_create_role(){
        Role role = new Role();
        role.setName("test");
        roleService.create(role);
        Assert.assertEquals(1,roleService.listRoles().size());
    }

    @Test
    public void should_update_role(){
        Role role = new Role();
        role.setName("test");
        Role role1 = roleService.create(role);
        role1.setName("test_update");
        roleService.update(role1);
        Assert.assertEquals("test_update", role1.getName());
    }

    @Test
    public void should_delete_role(){
        Role role = new Role();
        role.setName("test");
        roleService.create(role);

        roleService.delete("test");
        Assert.assertEquals(0, roleService.listRoles().size());
    }

    @Test
    public void should_list_roles(){
        Role role = new Role();
        role.setName("test");
        roleService.create(role);

        Assert.assertEquals(1, roleService.listRoles().size());
    }
}