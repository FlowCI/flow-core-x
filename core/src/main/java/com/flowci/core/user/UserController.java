/*
 * Copyright 2019 fir.im
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

package com.flowci.core.user;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.auth.service.AuthService;
import com.flowci.core.user.domain.*;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.ArgumentException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author yang
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final String DefaultPage = "0";

    private static final String DefaultSize = "20";

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @GetMapping
    @Action(UserAction.LIST_ALL)
    public Page<User> listAll(@RequestParam(required = false, defaultValue = DefaultPage) int page,
                              @RequestParam(required = false, defaultValue = DefaultSize) int size) {
        return userService.list(PageRequest.of(page, size));
    }

    @PostMapping
    @Action(UserAction.CREATE_USER)
    public User create(@Validated @RequestBody CreateUser body) {
        return userService.create(body.getEmail(), body.getPasswordOnMd5(), body.getUserRole());
    }

    @PostMapping("/change/password")
    @Action(UserAction.CHANGE_PASSWORD)
    public void changePassword(@Validated @RequestBody ChangePassword body) {
        if (Objects.equals(body.getNewOne(), body.getConfirm())) {
            userService.changePassword(body.getOld(), body.getNewOne());
            authService.logout();
            return;
        }

        throw new ArgumentException("the confirm password is inconsistent");
    }

    @PostMapping("/change/role")
    @Action(UserAction.CHANGE_ROLE)
    public void changeRole(@Validated @RequestBody ChangeRole body) {
        userService.changeRole(body.getEmail(), body.getUserRole());
    }

    @DeleteMapping
    @Action(UserAction.DELETE_USER)
    public User delete(@Validated @RequestBody DeleteUser body) {
        return userService.delete(body.getEmail());
    }
}
