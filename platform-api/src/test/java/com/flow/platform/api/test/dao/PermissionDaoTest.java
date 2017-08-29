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

import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.domain.user.GroupPermission;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class PermissionDaoTest extends TestBase {

    @Autowired
    private PermissionDao permissionDao;

    @Test
    public void should_create_permission() {

        Permission permission = new Permission();
        permission.setAction("test");
        permission.setName("test");
        permission.setDescription("test desc");
        permission.setTag(GroupPermission.ADMIN);

        permissionDao.save(permission);

        Assert.assertEquals("test", permission.getName());
        Assert.assertEquals("test desc", permission.getDescription());
        Assert.assertEquals(1, permissionDao.list().size());
    }

    @Test
    public void should_update_permission_success() {
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setName("test");
        permission.setDescription("test desc");
        permission.setTag(GroupPermission.ADMIN);

        Permission permission1 = permissionDao.save(permission);
        permission1.setName("test1");
        permission1.setTag(GroupPermission.SUPERADMIN);
        permissionDao.update(permission1);

        Assert.assertEquals("test1", permission1.getName());
        Assert.assertEquals(GroupPermission.SUPERADMIN, permission1.getTag());
    }

    @Test
    public void should_delete_permission_success() {
        Permission permission = new Permission();
        permission.setAction("test");
        permission.setName("test");
        permission.setDescription("test desc");
        permission.setTag(GroupPermission.ADMIN);

        permissionDao.save(permission);

        permissionDao.delete(permission);
        Assert.assertEquals(0, permissionDao.list().size());
    }

}
