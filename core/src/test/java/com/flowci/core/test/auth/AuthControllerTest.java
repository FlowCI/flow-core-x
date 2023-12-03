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

package com.flowci.core.test.auth;

import com.flowci.common.exception.AuthenticationException;
import com.flowci.common.exception.ErrorCode;
import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.auth.service.AuthService;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class AuthControllerTest extends SpringScenario {

    private User user;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void createUser() {
        authHelper.enableAuth();
        user = userService.create("test@flow.ci", "12345", User.Role.Admin);
    }

    @AfterEach
    void reset() {
        authHelper.disableAuth();
    }

    @Test
    void should_login_and_logout_successfully() throws Exception {
        // init: log in
        ResponseMessage<Tokens> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData().getToken();
        assertNotNull(token);

        String refreshToken = message.getData().getRefreshToken();
        assertNotNull(refreshToken);

        assertEquals(user, sessionManager.get());
        assertTrue(authService.set(token));

        // when: request logout
        var logoutMsg = authHelper.logout(token);
        assertEquals(StatusCode.OK, logoutMsg.getCode());

        // then: should throw new AuthenticationException("Not logged in") exception
        assertThrows(AuthenticationException.class, () -> sessionManager.get());
    }

    @Test
    void should_login_and_return_402_with_invalid_password() throws Exception {
        ResponseMessage<Tokens> message = authHelper.login(user.getEmail(), "wrong..");

        assertEquals(ErrorCode.AUTH_FAILURE, message.getCode());
        assertEquals("Invalid password", message.getMessage());
    }

    @Test
    void should_login_and_token_expired() throws Exception {
        // init: log in
        ResponseMessage<Tokens> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData().getToken();

        // when: wait for expire > 5s from properties file
        ThreadHelper.sleep(10000);

        // then: token should be expired
        assertFalse(authService.set(token));
    }

    @Test
    void should_refresh_token_while_NOT_expired() throws Exception {
        ResponseMessage<Tokens> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());

        // when:
        ThreadHelper.sleep(1000);
        ResponseMessage<Tokens> refreshed = authHelper.refresh(message.getData());

        // then:
        assertNotEquals(message.getData().getToken(), refreshed.getData().getToken());
        assertEquals(message.getData().getRefreshToken(), refreshed.getData().getRefreshToken());

        assertTrue(authService.set(refreshed.getData().getToken()));
        assertEquals(user, sessionManager.get());
    }

    @Test
    void should_refresh_token_while_expired() throws Exception {
        ResponseMessage<Tokens> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());

        // when:
        ThreadHelper.sleep(8000);
        ResponseMessage<Tokens> refreshed = authHelper.refresh(message.getData());

        // then:
        assertNotEquals(message.getData().getToken(), refreshed.getData().getToken());
        assertEquals(message.getData().getRefreshToken(), refreshed.getData().getRefreshToken());

        assertTrue(authService.set(refreshed.getData().getToken()));
        assertEquals(user, sessionManager.get());
    }
}
