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

import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.PermissionKey;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author lhl
 */
public class PermissionDaoTest extends TestBase {

    @Test
    public void should_create_permission() {
        Permission permission = new Permission(1, "show");
        permissionDao.save(permission);

        Assert.assertEquals("show", permission.getAction());
        Assert.assertEquals(1, permissionDao.list().size());
    }

    @Test
    public void should_find_permissions_by_role_id() {
        // when
        final int roleId = 1;

        Permission permissionForShow = new Permission(roleId, "show");
        permissionDao.save(permissionForShow);

        Permission permissionForCreate = new Permission(roleId, "create");
        permissionDao.save(permissionForCreate);

        // then:
        Assert.assertEquals(2, permissionDao.list(roleId).size());
        Assert.assertEquals(2L, permissionDao.numOfAction(roleId).longValue());
    }

    @Test
    public void should_find_permissions_by_action() {
        // when
        final String action = "create-flow";

        Permission actionForFirstRole = new Permission(1, action);
        permissionDao.save(actionForFirstRole);

        Permission actionForOtherRole = new Permission(2, action);
        permissionDao.save(actionForOtherRole);

        // then:
        Assert.assertEquals(2, permissionDao.list(action).size());
        Assert.assertEquals(2L, permissionDao.numOfRole(action).longValue());
    }

    @Test
    public void should_delete_users_roles() {
        PermissionKey rolePermissionKey = new PermissionKey(1, "show");
        Permission rolesPermissions = new Permission(rolePermissionKey);
        permissionDao.save(rolesPermissions);

        permissionDao.delete(rolesPermissions);
        Assert.assertEquals(0, permissionDao.list().size());
    }
}
