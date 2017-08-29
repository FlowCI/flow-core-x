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

import com.flow.platform.api.dao.user.UsersRolesDao;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.domain.user.UsersRoles;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "usersRolesService")
public class UsersRolesServiceImpl implements UsersRolesService {

    @Autowired
    private UsersRolesDao usersRolesDao;

    @Override
    public UsersRoles create(UsersRoles usersRoles) {
        usersRolesDao.save(usersRoles);
        return usersRoles;
    }

//    @Override
//    public UsersRoles update(UsersRoles usersRoles) {
//        usersRolesDao.update(usersRoles);
//        return usersRoles;
//    }

    @Override
    public void delete(UserRoleKey userRoleKey) {
        UsersRoles usersRoles = findUsersRolesByKey(userRoleKey);
        usersRolesDao.delete(usersRoles);
    }

    @Override
    public List<UsersRoles> listUsersRoles() {
        return usersRolesDao.list();
    }

    @Override
    public List<UsersRoles> listUsersRolesByEmail(String email) {
        return usersRolesDao.list(email);
    }


    private UsersRoles findUsersRolesByKey(UserRoleKey userRoleKey) {
        return usersRolesDao.get(userRoleKey);
    }

}
