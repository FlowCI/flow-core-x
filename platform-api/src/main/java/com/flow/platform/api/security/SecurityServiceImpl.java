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
package com.flow.platform.api.security;

import com.flow.platform.api.domain.user.RolesPermissions;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UsersRoles;
import com.flow.platform.api.service.user.RolesPermissionsService;
import com.flow.platform.api.service.user.UsersRolesService;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */
@Service
@Transactional
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    private UsersRolesService usersRolesService;

    @Autowired
    private RolesPermissionsService rolesPermissionsService;

    @Override
    public boolean hasPermissions(User user, String action) {

        List<UsersRoles> resources = usersRolesService.listUsersRolesByEmail(user.getEmail());

        List<Integer> roleIds = new ArrayList<>();

        for (UsersRoles u : resources) {
            roleIds.add(u.getRoleId());
        }

        List<String> permissionAction = new ArrayList<>();

        for (Integer roleId : roleIds) {
            List<RolesPermissions> rolesPermissions = rolesPermissionsService.listRolesPermissionsByRoleId(roleId);
            for (RolesPermissions rp : rolesPermissions) {
                permissionAction.add(rp.getAction());
            }
        }

        return permissionAction.contains(action);

    }
}
