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

import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service
public class UserSecurityService implements UserDetailsService {

    private final static Logger LOGGER = new Logger(UserSecurityService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService rolesPermissionsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username;
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Cannot find user by email: " + email);
        }

        return null;

//        List<UserRole> resources= usersRolesService.listUsersRolesByEmail(user.getEmail());
//
//        List<Integer> roleIds = new ArrayList<>();
//
//        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//
//        for (UserRole u : resources) {
//            roleIds.add(u.getRoleId());
//        }
//
//        for (Integer roleId : roleIds) {
//            List<RolesPermissions> rolesPermissions = rolesPermissionsService.listRolesPermissionsByRoleId(roleId);
//            for (RolesPermissions rp : rolesPermissions) {
//                authorities.add(new SimpleGrantedAuthority(rp.getAction()));
//            }
//        }
//
//        return new org.springframework.security.core.userdetails.User(user.getUsername(),
//            user.getPassword(), authorities);
    }
}


