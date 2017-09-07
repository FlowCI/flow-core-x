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

import com.flow.platform.api.domain.user.PermissionKey;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.core.dao.BaseDao;
import java.util.List;

/**
 * @author lhl
 */
public interface PermissionDao extends BaseDao<PermissionKey, Permission> {

    /**
     * Get list of action by role id
     */
    List<String> list(Integer roleId);

    /**
     * Get list of role id by action
     */
    List<Integer> list(String action);

    /**
     * Get num of role assign to action
     */
    Long numOfRole(String action);

    /**
     * Get num of action assign to role
     */
    Long numOfAction(Integer roleId);
}
