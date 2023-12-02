/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.user.service;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.user.dao.UserDao;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.domain.User.Role;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.common.exception.StatusException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author yang
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final static String UserCacheName = "users";

    @Autowired
    private UserDao userDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private CacheManager defaultCacheManager;

    @Override
    public Page<User> list(Pageable pageable) {
        return userDao.findAll(pageable);
    }

    @Override
    public List<User> list(Collection<String> emails) {
        Iterable<User> all = userDao.findAllByEmailIn(emails);
        return Lists.newArrayList(all);
    }

    @Override
    public Optional<User> defaultAdmin() {
        return userDao.findByRoleAndDefaultAdmin(Role.Admin, true);
    }

    @Override
    @CachePut(cacheManager = "defaultCacheManager", value = UserCacheName, key = "#email")
    public User createDefaultAdmin(String email, String passwordOnMd5) {
        if (defaultAdmin().isPresent()) {
            throw new StatusException("Default admin has been created");
        }

        return create(email, passwordOnMd5, Role.Admin, Boolean.TRUE);
    }

    @Override
    @CachePut(cacheManager = "defaultCacheManager", value = UserCacheName, key = "#email")
    public User create(String email, String passwordOnMd5, User.Role role) {
        return create(email, passwordOnMd5, role, null);
    }

    @Override
    @Cacheable(cacheManager = "defaultCacheManager", value = UserCacheName, key = "#email")
    public User getByEmail(String email) {
        User user = userDao.findByEmail(email);
        if (Objects.isNull(user)) {
            throw new NotFoundException("User with email {0} is not existed", email);
        }
        return user;
    }

    @Override
    public void changePassword(User user, String oldOnMd5, String newOnMd5) {
        if (Objects.equals(user.getPasswordOnMd5(), oldOnMd5)) {
            user.setPasswordOnMd5(newOnMd5);
            userDao.save(user);
            Cache cache = defaultCacheManager.getCache(UserCacheName);
            if (cache != null) {
                cache.evict(user.getEmail());
            }
            return;
        }

        throw new ArgumentException("The password is incorrect");
    }

    @Override
    @CacheEvict(cacheManager = "defaultCacheManager", value = UserCacheName, key = "#email")
    public void changeRole(String email, Role newRole) {
        User user = getByEmail(email);
        if (user.isDefaultAdmin()) {
            throw new StatusException("Default admin user role cannot be changed");
        }

        if (user.getRole().equals(newRole)) {
            return;
        }

        user.setRole(newRole);
        userDao.save(user);
    }

    @Override
    @CacheEvict(cacheManager = "defaultCacheManager", value = UserCacheName, key = "#email")
    public User delete(String email) {
        User user = getByEmail(email);
        if (user.isDefaultAdmin()) {
            throw new StatusException("Default admin user cannot be deleted");
        }

        userDao.delete(user);
        eventManager.publish(new UserDeletedEvent(this, user));
        return user;
    }

    private User create(String email, String md5pw, Role role, Boolean isDefaultAdmin) {
        try {
            User user = new User(email, md5pw, role, isDefaultAdmin);
            return userDao.insert(user);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Email {0} is already existed", email);
        }
    }
}
