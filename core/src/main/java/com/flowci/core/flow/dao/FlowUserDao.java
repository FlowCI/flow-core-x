/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.flow.dao;

import java.util.List;
import java.util.Set;

public interface FlowUserDao {

    void create(String flowId);

    /**
     * Remove flow user list
     */
    void delete(String flowId);

    /**
     * Find all flows by users email
     */
    List<String> findAllFlowsByUserEmail(String email);

    /**
     * Find all users by flow id
     */
    List<String> findAllUsers(String flowId);

    /**
     * Batch insert users
     */
    void insert(String flowId, Set<String> emails);

    /**
     * Batch remove users
     */
    void remove(String flowId, Set<String> emails);

    /**
     * Check user is existed
     */
    boolean exist(String flowId, String emails);
}
