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

import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import java.util.List;

/**
 * @author lhl
 */
public interface RoleService {


    Role find(String name);

    /**
     * Create role by name and desc
     *
     * @param role role unique name
     * @param desc role description, can be null
     */
    Role create(String role, String desc);

    /**
     * Update role name or desc
     */
    void update(Role role);

    /**
     * Delete role
     */
    void delete(String role);

    /**
     * List all roles
     */
    List<Role> list();

    /**
     * List roles for user
     */
    List<Role> list(User user);

    /**
     * List users for role
     */
    List<User> list(String role);

    /**
     * Assign user to role
     */
    void assign(User user, String role);

    /**
     * Un-assign a user form role
     */
    void unAssign(User user, String role);
}
