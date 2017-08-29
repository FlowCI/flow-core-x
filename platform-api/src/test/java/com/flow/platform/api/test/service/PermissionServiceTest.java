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

import com.flow.platform.api.domain.user.GroupPermission;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class PermissionServiceTest extends TestBase {

    @Autowired
    private PermissionService permissionService;

    @Test
    public void should_create_permission(){
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setName("testName");
        permission.setTag(GroupPermission.SUPERADMIN);
        permissionService.create(permission);
        Assert.assertEquals(1,permissionService.listPermissions().size());
    }

    @Test
    public void should_update_role(){
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setTag(GroupPermission.SUPERADMIN);
        Permission permission1 = permissionService.create(permission);
        permission1.setTag(GroupPermission.DEFAULT);
        permissionService.update(permission1);
        Assert.assertEquals(GroupPermission.DEFAULT, permission1.getTag());
    }

    @Test
    public void should_delete_role(){
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setTag(GroupPermission.MANAGER);
        permissionService.create(permission);

        permissionService.delete("test");
        Assert.assertEquals(0, permissionService.listPermissions().size());
    }

    @Test
    public void should_list_roles(){
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setTag(GroupPermission.MANAGER);
        permissionService.create(permission);

        Assert.assertEquals(1, permissionService.listPermissions().size());
    }

}
