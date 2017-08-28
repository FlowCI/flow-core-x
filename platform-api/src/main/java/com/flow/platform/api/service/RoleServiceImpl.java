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
package com.flow.platform.api.service;

import com.flow.platform.api.dao.RoleDao;
import com.flow.platform.api.domain.Role;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "roleService")

public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleDao roleDao;

    @Override
    public List<Role> listRoles() {
        return roleDao.list();
    }

    @Override
    public Role create(Role role) {
        if (findRoleByName(role.getName()) != null) {
            throw new IllegalParameterException(String.format("name is already present"));
        } else {
            roleDao.save(role);
            return role;
        }
    }

    @Override
    public Role update(Role role) {
        Role role1 = findRoleByName(role.getName());
        role1.setContent(role.getContent());
        roleDao.update(role1);
        return role1;
    }

    @Override
    public void delete(String name) {
        Role role = findRoleByName(name);
        roleDao.delete(role);
    }

    private Role findRoleByName(String name) {
        for (Role role : roleDao.list()) {
            if (role.getName().equals(name)) {
                return role;
            }
        }
        return null;
    }
}
