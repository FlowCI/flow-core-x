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

package com.flowci.core.test.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.helper.HashingHelper;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.*;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.common.exception.AuthenticationException;
import com.flowci.common.exception.ErrorCode;
import com.flowci.common.exception.NotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class UserControllerTest extends SpringScenario {

    private static final TypeReference<ResponseMessage<User>> UserType =
            new TypeReference<>() {
            };

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionManager sessionManager;

    @Test(expected = AuthenticationException.class)
    public void should_change_password_for_current_user_successfully() throws Exception {
        var dummy = new User();
        dummy.setId("1111");
        dummy.setPasswordOnMd5(HashingHelper.md5("22222"));
        dummy.setRole(User.Role.Admin);
        dummy.setEmail("test@flow.ci");
        sessionManager.set(dummy);

        User user = sessionManager.get();

        String newPwOnMd5 = HashingHelper.md5("11111");
        ChangePassword body = new ChangePassword();
        body.setOld(user.getPasswordOnMd5());
        body.setNewOne(newPwOnMd5);
        body.setConfirm(newPwOnMd5);

        // when:
        var message = mockMvcHelper.expectSuccessAndReturnClass(
                post("/users/change/password")
                        .content(objectMapper.writeValueAsBytes(body))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);

        Assert.assertEquals(StatusCode.OK, message.getCode());

        // then:
        Assert.assertEquals(newPwOnMd5, userService.getByEmail(user.getEmail()).getPasswordOnMd5());

        // then: throw AuthenticationException
        sessionManager.get();
    }

    @Test
    public void should_create_user_successfully() throws Exception {
        createUser("test.create@flow.ci", "111111", User.Role.Admin);

        User created = userService.getByEmail("test.create@flow.ci");
        Assert.assertEquals(User.Role.Admin, created.getRole());
        Assert.assertNotNull(created.getCreatedBy());
        Assert.assertNotNull(created.getCreatedAt());
        Assert.assertNotNull(created.getUpdatedAt());
    }

    @Test
    public void should_fail_to_create_user_if_mail_invalid() throws Exception {
        ResponseMessage message = createUser("test.flow.ci", "111111", User.Role.Admin);

        Assert.assertEquals(ErrorCode.INVALID_ARGUMENT, message.getCode());
    }

    @Test
    public void should_change_user_role_successfully() throws Exception {
        ResponseMessage<User> msg = createUser("test.change.role@flow.ci", "12345", User.Role.Developer);
        User user = msg.getData();
        Assert.assertEquals(User.Role.Developer, user.getRole());

        ChangeRole body = new ChangeRole();
        body.setEmail(user.getEmail());
        body.setRole(User.Role.Admin.name());

        ResponseMessage<?> message = mockMvcHelper.expectSuccessAndReturnClass(
                post("/users/change/role")
                        .content(objectMapper.writeValueAsBytes(body))
                        .contentType(MediaType.APPLICATION_JSON)
                , ResponseMessage.class);

        Assert.assertEquals(StatusCode.OK, message.getCode());
        Assert.assertEquals(User.Role.Admin, userService.getByEmail(user.getEmail()).getRole());
    }

    @Test(expected = NotFoundException.class)
    public void should_delete_user_and_send_delete_event_out() throws Exception {
        ResponseMessage<User> created = createUser("test.delete@flow.ci", "12345", User.Role.Developer);

        DeleteUser body = new DeleteUser();
        body.setEmail(created.getData().getEmail());

        final CountDownLatch counter = new CountDownLatch(1);
        addEventListener((ApplicationListener<UserDeletedEvent>) event -> counter.countDown());

        ResponseMessage<User> deleted = mockMvcHelper.expectSuccessAndReturnClass(
                delete("/users")
                        .content(objectMapper.writeValueAsBytes(body))
                        .contentType(MediaType.APPLICATION_JSON), UserType);

        Assert.assertTrue(counter.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(created.getData(), deleted.getData());

        // then: throw not found exception
        userService.getByEmail(created.getData().getEmail());
    }

    private ResponseMessage<User> createUser(String email, String password, User.Role role) throws Exception {
        CreateUser body = new CreateUser();
        body.setEmail(email);
        body.setPasswordOnMd5(HashingHelper.md5(password));
        body.setRole(role.name());

        return mockMvcHelper.expectSuccessAndReturnClass(post("/users")
                .content(objectMapper.writeValueAsBytes(body))
                .contentType(MediaType.APPLICATION_JSON), UserType);
    }
}
