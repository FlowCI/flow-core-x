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

package com.flowci.core.common.manager;

import com.flowci.core.user.domain.User;
import com.flowci.common.exception.AuthenticationException;
import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionManager implements AuditorAware<String> {

    private final ThreadLocal<User> currentUser = new ThreadLocal<>();

    private static final String DefaultCreator = "System";

    @NonNull
    @Override
    public Optional<String> getCurrentAuditor() {
        if (exist()) {
            return Optional.of(currentUser.get().getEmail());
        }
        return Optional.of(DefaultCreator);
    }

    public User get() {
        User user = currentUser.get();
        if (!exist()) {
            throw new AuthenticationException("Not logged in");
        }
        return user;
    }

    public String getUserEmail() {
        return get().getEmail();
    }

    public void set(User user) {
        currentUser.set(user);
    }

    public User remove() {
        User user = currentUser.get();
        currentUser.remove();
        return user;
    }

    public boolean exist() {
        return currentUser.get() != null;
    }
}
