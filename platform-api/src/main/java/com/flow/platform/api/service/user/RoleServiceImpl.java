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

import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserRole;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */

@Service
@Transactional
public class RoleServiceImpl extends CurrentUser implements RoleService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Override
    public Role find(Integer roleId) {
        Role role = roleDao.get(roleId);
        if (role == null) {
            throw new IllegalParameterException(String.format("The id of role '%s' is doesn't existed", roleId));
        }
        return role;
    }

    @Override
    public Role find(String name) {
        Role role = roleDao.get(name);
        if (role == null) {
            throw new IllegalParameterException(String.format("The name of role '%s' is doesn't existed", name));
        }
        return role;
    }

    @Override
    public Role create(String name, String desc) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalParameterException("The name of role must be provided");
        }

        Role existRole = roleDao.get(name);
        if (existRole != null) {
            throw new IllegalParameterException("The name of role is already presented");
        }
        Role role = new Role(name, desc);
        role.setCreatedBy(currentUser().getEmail());
        roleDao.save(role);
        return role;
    }

    @Override
    public void update(Role role) {
        role.setCreatedBy(currentUser().getEmail());
        roleDao.update(role);
    }

    @Override
    public void delete(Integer roleId) {
        Role role = find(roleId);
        Long numOfUser = userRoleDao.numOfUser(role.getId());

        if (numOfUser > 0L) {
            final String err = String.format("Cannot delete role '%s' since has '%s' assigned", roleId, numOfUser);
            throw new IllegalStatusException(err);
        }

        roleDao.delete(role);
    }

    @Override
    public List<Role> list() {
        return roleDao.list();
    }

    @Override
    public List<User> list(String role) {
        Role roleObj = find(role);
        List<String> userEmails = userRoleDao.list(roleObj.getId());
        if (userEmails.isEmpty()) {
            return new ArrayList<>(0);
        }
        return userDao.list(userEmails);
    }

    @Override
    public List<Role> list(User user) {
        List<Integer> roleIds = userRoleDao.list(user.getEmail());
        if (roleIds.isEmpty()) {
            return new ArrayList<>(0);
        }
        return roleDao.list(roleIds);
    }

    @Override
    public void assign(User user, Role role) {
        UserRole userRole = new UserRole(role.getId(), user.getEmail());
        userRole.setCreatedBy(currentUser().getEmail());
        userRoleDao.save(userRole);
    }

    @Override
    public void unAssign(User user) {
        userRoleDao.delete(user.getEmail());
    }

    @Override
    public void unAssign(User user, Role role) {
        UserRole userRole = userRoleDao.get(new UserRoleKey(role.getId(), user.getEmail()));
        if (userRole != null) {
            userRoleDao.delete(userRole);
        }
    }

}
