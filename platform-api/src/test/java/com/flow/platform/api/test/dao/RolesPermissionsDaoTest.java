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

import com.flow.platform.api.dao.user.RolesPermissionsDao;
import com.flow.platform.api.domain.user.RolePermissionKey;
import com.flow.platform.api.domain.user.RolesPermissions;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class RolesPermissionsDaoTest extends TestBase {

    @Autowired
    private RolesPermissionsDao rolesPermissionsDao;

    @Test
    public void should_create_role_permission() {
        RolePermissionKey rolePermissionKey = new RolePermissionKey(1, "show");
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);
        rolesPermissionsDao.save(rolesPermissions);

        Assert.assertEquals("show",rolesPermissions.getAction());
        Assert.assertEquals(1, rolesPermissionsDao.list().size());
    }

    @Test
    public void should_find_role_permissions_by_roleId() {
        RolePermissionKey rolePermissionKey = new RolePermissionKey(1, "show");
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);
        rolesPermissionsDao.save(rolesPermissions);

        RolePermissionKey rolePermissionKey1 = new RolePermissionKey(1, "create");
        RolesPermissions rolesPermissions1 = new RolesPermissions(rolePermissionKey1);
        rolesPermissionsDao.save(rolesPermissions1);

        Assert.assertEquals(2, rolesPermissionsDao.list(1).size());
    }

    @Test
    public void should_delete_users_roles() {
        RolePermissionKey rolePermissionKey = new RolePermissionKey(1, "show");
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);
        rolesPermissionsDao.save(rolesPermissions);

        rolesPermissionsDao.delete(rolesPermissions);

        Assert.assertEquals(0, rolesPermissionsDao.list().size());
    }


}
