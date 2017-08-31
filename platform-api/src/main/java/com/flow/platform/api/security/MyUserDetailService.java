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

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.domain.user.RolesPermissions;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UsersRoles;
import com.flow.platform.api.service.user.RolesPermissionsService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.service.user.UsersRolesService;
import com.flow.platform.util.Logger;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service
public class MyUserDetailService implements UserDetailsService {

    private final static Logger LOGGER = new Logger(MyUserDetailService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UsersRolesService usersRolesService;

    @Autowired
    private RolesPermissionsService rolesPermissionsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("no user");
        }


        List<UsersRoles> resources= usersRolesService.listUsersRolesByEmail(user.getEmail());

        List<Integer> roleIds = new ArrayList<>();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        for (UsersRoles u : resources) {
            roleIds.add(u.getRoleId());
        }

        for (Integer roleId : roleIds) {
            List<RolesPermissions> rolesPermissions = rolesPermissionsService.listRolesPermissionsByRoleId(roleId);
            for (RolesPermissions rp : rolesPermissions) {
                authorities.add(new SimpleGrantedAuthority(rp.getAction()));
            }
        }

        return new org.springframework.security.core.userdetails.User(user.getUsername(),
            user.getPassword(), authorities);
    }
}


