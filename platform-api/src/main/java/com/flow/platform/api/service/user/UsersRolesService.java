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

import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.domain.user.UsersRoles;
import java.util.List;

/**
 * @author lhl
 */
public interface UsersRolesService {

    UsersRoles create(UsersRoles usersRoles);

//    UsersRoles update(UsersRoles usersRoles);

    void delete(UserRoleKey userRoleKey);

    List<UsersRoles> listUsersRoles();

    List<UsersRoles> listUsersRolesByEmail(String email);



}
