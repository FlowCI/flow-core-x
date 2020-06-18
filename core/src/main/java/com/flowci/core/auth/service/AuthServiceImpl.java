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
import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.auth.helper.JwtHelper;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.AuthenticationException;
import com.flowci.util.HashingHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppProperties.Auth authProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private Cache onlineUsersCache;

    @Autowired
    private Cache refreshTokenCache;

    @Autowired
    private PermissionMap permissionMap;

    @Override
    public Boolean isEnabled() {
        return authProperties.getEnabled();
    }

    @Override
    public Tokens login(String email, String passwordOnMd5) {
        User user = userService.getByEmail(email);

        if (!Objects.equals(user.getPasswordOnMd5(), passwordOnMd5)) {
            throw new AuthenticationException("Invalid password");
        }

        // create token
        String token = JwtHelper.create(user, authProperties.getExpireSeconds());
        sessionManager.set(user);
        onlineUsersCache.put(email, user);

        // create refresh token
        String refreshToken = HashingHelper.md5(email + passwordOnMd5);
        refreshTokenCache.put(email, refreshToken);

        return new Tokens(token, refreshToken);
    }

    @Override
    public void logout() {
        User user = sessionManager.remove();
        onlineUsersCache.evict(user.getEmail());
        refreshTokenCache.evict(user.getEmail());
    }

    @Override
    public boolean hasPermission(Action action) {
        // everyone has permission if no action defined
        if (Objects.isNull(action)) {
            return true;
        }

        // admin has all permission
        User user = sessionManager.get();
        return permissionMap.hasPermission(user.getRole(), action.value());
    }

    @Override
    public Tokens refresh(Tokens tokens) {
        String token = tokens.getToken();
        String refreshToken = tokens.getRefreshToken();
        String email = JwtHelper.decode(token);

        if (!Objects.equals(refreshTokenCache.get(email, String.class), refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // find user from online cache or database
        User user = onlineUsersCache.get(email, User.class);
        if (Objects.isNull(user)) {
            user = userService.getByEmail(email);
        }

        boolean verify = JwtHelper.verify(token, user, false);
        if (verify) {
            String newToken = JwtHelper.create(user, authProperties.getExpireSeconds());
            onlineUsersCache.put(email, user);
            return new Tokens(newToken, refreshToken);
        }

        throw new AuthenticationException("Invalid token");
    }

    @Override
    public boolean set(String token) {
        Optional<User> optional = get(token);
        if (optional.isPresent()) {
            sessionManager.set(optional.get());
            return true;
        }
        return false;
    }

    @Override
    public Optional<User> get(String token) {
        String email = JwtHelper.decode(token);

        User user = onlineUsersCache.get(email, User.class);
        if (Objects.isNull(user)) {
            return Optional.empty();
        }

        boolean verify = JwtHelper.verify(token, user, true);
        if (verify) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    @Override
    public boolean setAsDefaultAdmin() {
        User defaultAdmin = userService.defaultAdmin();
        sessionManager.set(defaultAdmin);
        return true;
    }
}
