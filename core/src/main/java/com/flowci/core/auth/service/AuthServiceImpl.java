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
import com.flowci.core.auth.dao.UserAuthDao;
import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.auth.domain.UserAuth;
import com.flowci.core.auth.helper.JwtHelper;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.AuthenticationException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.HashingHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppProperties.Auth authProperties;

    @Autowired
    private UserAuthDao userAuthDao;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionManager sessionManager;

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

        String token = JwtHelper.create(user, authProperties.getExpireSeconds());
        String refreshToken = HashingHelper.md5(email + passwordOnMd5);

        UserAuth auth = new UserAuth();
        auth.setEmail(email);
        auth.setToken(token);
        auth.setRefreshToken(refreshToken);
        save(auth);

        sessionManager.set(user);
        return new Tokens(token, refreshToken);
    }

    @Override
    public void logout() {
        User user = sessionManager.remove();
        userAuthDao.deleteByEmail(user.getEmail());
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

        Optional<UserAuth> auth = userAuthDao.findByEmail(email);
        if (!auth.isPresent()) {
            throw new AuthenticationException("Not signed in");
        }

        UserAuth userAuth = auth.get();
        if (!Objects.equals(userAuth.getRefreshToken(), refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // find user from online cache or database
        User user = getUser(email);
        if (Objects.isNull(user)) {
            throw new AuthenticationException("User not found");
        }

        boolean verify = JwtHelper.verify(token, user, false);
        if (verify) {
            String newToken = JwtHelper.create(user, authProperties.getExpireSeconds());
            userAuthDao.update(userAuth.getId(), newToken);
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

        User user = getUser(email);
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
    public Set<String> getActions(User.Role role) {
        return permissionMap.get(role);
    }

    private User getUser(String email) {
        try {
            return userService.getByEmail(email);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void save(UserAuth auth) {
        Optional<UserAuth> optional = userAuthDao.findByEmail(auth.getEmail());
        if (optional.isPresent()) {
            UserAuth exist = optional.get();
            userAuthDao.update(exist.getId(), auth.getToken(), auth.getRefreshToken());
            return;
        }
        userAuthDao.insert(auth);
    }
}
