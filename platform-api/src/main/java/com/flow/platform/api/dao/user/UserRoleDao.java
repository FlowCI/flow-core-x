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
package com.flow.platform.api.dao.user;

import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.domain.user.UserRole;
import com.flow.platform.core.dao.BaseDao;
import java.util.List;

/**
 * @author lhl
 */
public interface UserRoleDao extends BaseDao<UserRoleKey, UserRole> {

    /**
     * List roles id for user email
     */
    List<Integer> list(String email);

    /**
     * List user emails for role id
     */
    List<String> list(Integer roleId);

    /**
     * Get number of user for role
     */
    Long numOfUser(Integer roleId);

}
