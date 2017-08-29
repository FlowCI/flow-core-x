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
package com.flow.platform.api.service.user;

import com.flow.platform.api.dao.user.RolesPermissionsDao;
import com.flow.platform.api.domain.user.RolePermissionKey;
import com.flow.platform.api.domain.user.RolesPermissions;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "rolesPermissionsService")
public class RolesPermissionsServiceImpl implements RolesPermissionsService {

    @Autowired
    private RolesPermissionsDao rolesPermissionsDao;

    @Override
    public RolesPermissions create(RolesPermissions rolesPermissions) {
        rolesPermissionsDao.save(rolesPermissions);
        return rolesPermissions;
    }

    @Override
    public void delete(RolePermissionKey rolePermissionKey) {
        RolesPermissions rolesPermissions = findRolesPermissionsByKey(rolePermissionKey);
        rolesPermissionsDao.delete(rolesPermissions);
    }

    @Override
    public List<RolesPermissions> listRolesPermissions() {
        return rolesPermissionsDao.list();
    }

    @Override
    public List<RolesPermissions> listRolesPermissionsByRoleId(Integer roleId) {
        return rolesPermissionsDao.list(roleId);
    }

    private RolesPermissions findRolesPermissionsByKey(RolePermissionKey rolePermissionKey) {
        return rolesPermissionsDao.get(rolePermissionKey);
    }

}
