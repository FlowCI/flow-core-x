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
package com.flow.platform.api.service;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.core.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */

public abstract class CurrentUser {

    @Autowired
    private ThreadLocal<User> currentUser;

    public User currentUser() {
        if (currentUser.get() == null) {
            throw new NotFoundException("Current user not found");
        }
        return currentUser.get();
    }
}
