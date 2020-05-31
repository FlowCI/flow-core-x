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

import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.exception.ErrorCode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class FlowAuthTest extends SpringScenario {

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Before
    public void init() {
        authHelper.enableAuth();
    }

    @After
    public void reset() {
        authHelper.disableAuth();
    }

    @Test
    public void should_get_exception_if_user_not_admin() throws Exception {
        User user = userService.create("test@flow.ci", "12345", User.Role.Developer);
        ResponseMessage<Tokens> login = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = login.getData().getToken();


        // when: create flow
        ResponseMessage response = mockMvcHelper.expectSuccessAndReturnClass(
                post("/flows/test").header("Token", token), ResponseMessage.class);

        // then: should return 403 with no permission message
        Assert.assertEquals(ErrorCode.NO_PERMISSION, response.getCode());
    }
}
