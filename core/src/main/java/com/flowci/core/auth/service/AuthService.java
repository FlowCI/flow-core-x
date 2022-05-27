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

package com.flowci.core.auth.service;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.user.domain.User;

import java.util.Optional;
import java.util.Set;

/**
 * 'login' ->
 * 'set' per request ->
 * 'get' current user | 'hasLogin' | 'getUserId' | 'refresh' ->
 * 'logout'
 */
public interface AuthService {

    Boolean isEnabled();

    /**
     * Login and return jwt token
     */
    Tokens login(String email, String passwordOnMd5);

    /**
     * Logout from current user
     */
    void logout();

    /**
     * Refresh and return new token
     */
    Tokens refresh(Tokens tokens);

    /**
     * Check current user has access right to given action
     * @param action can be null which means no permission control
     */
    boolean hasPermission(Action action);

    /**
     * Set current logged in user by token
     * @return Current user object or null if not verified
     */
    boolean set(String token);

    /**
     * Get current logged in user
     */
    Optional<User> get(String token);

    /**
     * Get action list by role
     */
    Set<String> getActions(User.Role role);
}
