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

package com.flowci.core.auth;

import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.auth.service.AuthService;
import com.flowci.core.user.domain.User;
import com.flowci.exception.AuthenticationException;
import com.google.common.base.Strings;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Tokens login(@RequestHeader("Authorization") String authorization) {
        User user = getFromAuthorization(authorization);
        return authService.login(user.getEmail(), user.getPasswordOnMd5());
    }

    @PostMapping("/refresh")
    public Tokens refresh(@Validated @RequestBody Tokens tokens) {
        return authService.refresh(tokens);
    }

    @PostMapping("/logout")
    public void logout() {
        authService.logout();
    }

    private User getFromAuthorization(String authorization) {
        if (Strings.isNullOrEmpty(authorization) || !authorization.startsWith("Basic")) {
            throw new AuthenticationException("Invalid request");
        }

        String base64 = authorization.substring("Basic".length()).trim();
        String content = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);

        final String[] values = content.split(":", 2);
        if (values.length != 2) {
            throw new AuthenticationException("Invalid request");
        }

        String email = values[0];
        String passwordOnMd5 = values[1];

        return new User(email, passwordOnMd5, null);
    }
}
