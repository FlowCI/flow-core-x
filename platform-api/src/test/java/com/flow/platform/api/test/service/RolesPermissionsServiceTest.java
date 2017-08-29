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

import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.domain.user.GroupPermission;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.RolePermissionKey;
import com.flow.platform.api.domain.user.RolesPermissions;
import com.flow.platform.api.service.user.RolesPermissionsService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class RolesPermissionsServiceTest extends TestBase {

    @Autowired
    private RolesPermissionsService rolesPermissionsService;

    @Autowired
    private PermissionDao permissionDao;

    @Autowired
    private RoleDao roleDao;

    private Permission permission;

    private Role role;

    @Before
    public void beforeTest() {
        permission = new Permission();
        permission.setAction("show");
        permission.setTag(GroupPermission.MANAGER);
        permissionDao.save(permission);

        role = new Role();
        role.setName("test_role");
        roleDao.save(role);
    }

    @Test
    public void should_create_roles_permission() {
        RolePermissionKey rolePermissionKey = new RolePermissionKey(role.getId(), permission.getAction());
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);

        rolesPermissionsService.create(rolesPermissions);

        Assert.assertEquals("show", rolesPermissions.getAction());
    }

    @Test
    public void should_delete_role_permission() {
        RolePermissionKey rolePermissionKey = new RolePermissionKey(role.getId(), permission.getAction());
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);

        rolesPermissionsService.create(rolesPermissions);

        rolesPermissionsService.delete(rolePermissionKey);

        Assert.assertEquals(0, rolesPermissionsService.listRolesPermissions().size());
    }

    @Test
    public void should_list_users_roles(){
        RolePermissionKey rolePermissionKey = new RolePermissionKey(role.getId(), permission.getAction());
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);

        rolesPermissionsService.create(rolesPermissions);
        Assert.assertEquals(1, rolesPermissionsService.listRolesPermissions().size());
    }

    @Test
    public void should_find_user_roles_by_email(){
        RolePermissionKey rolePermissionKey = new RolePermissionKey(role.getId(), permission.getAction());
        RolesPermissions rolesPermissions = new RolesPermissions(rolePermissionKey);

        rolesPermissionsService.create(rolesPermissions);
        Assert.assertEquals(1, rolesPermissionsService.listRolesPermissionsByRoleId(role.getId()).size());
    }

}
