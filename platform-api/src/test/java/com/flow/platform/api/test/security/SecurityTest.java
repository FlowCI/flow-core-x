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

package com.flow.platform.api.test.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.response.LoginResponse;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.initializers.UserRoleInit;
import com.flow.platform.api.security.AuthenticationInterceptor;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.test.TestBase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
public class SecurityTest extends TestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ActionService actionService;

    @Autowired
    private AuthenticationInterceptor authInterceptor;

    @Autowired
    private UserRoleInit userRoleInit;

    private User userForAdmin;

    private User userForUser;

    private User userWithoutAuthority;

    private final String flowName = "flow1";

    private final String password = "testPassword";

    @Before
    public void init() throws Throwable {
        authInterceptor.enable();

        // init system roles, actions and permissions
        userRoleInit.doStart();

        Role ymlOperator = new Role("ROLE_YML", null);
        roleDao.save(ymlOperator);
        permissionService.assign(ymlOperator, Sets.newHashSet(actionService.find(Actions.FLOW_YML.name())));

        // init mock user
        userForAdmin = userService.register(new User("test1@flow.ci", "test1", password),
            ImmutableList.of(SysRole.ADMIN.name()),
            false,
            ImmutableList.of("flow2"));

        userForUser = userService.register(new User("test2@flow.ci", "test2", password),
            ImmutableList.of(SysRole.USER.name()),
            false,
            ImmutableList.of(flowName));

        userWithoutAuthority = userService.register(new User("test3@flow.ci", "test3", password),
            ImmutableList.of("ROLE_YML"),
            false,
            ImmutableList.of(flowName));

    }

    @Test
    public void should_raise_401_when_access_url_from_user_without_role() throws Throwable {
        userWithoutAuthority.setPassword(password);

        // test to list flows
        this.mockMvc.perform(requestWithUser(get("/flows"), userWithoutAuthority))
            .andExpect(status().isUnauthorized());

        // test to get flow detail
        this.mockMvc.perform(requestWithUser(get("/flows/" + flowName + "/show"), userWithoutAuthority))
            .andExpect(status().isUnauthorized());

        // test to crate flow
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName), userWithoutAuthority))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void should_user_role_operate_show_and_yml() throws Throwable {
        // given: crate flow
        createRootFlow(flowName, "yml/demo_flow.yaml");

        // set to original password before md5
        userForUser.setPassword(password);

        // test to list flows
        this.mockMvc.perform(requestWithUser(get("/flows"), userForUser))
            .andExpect(status().isOk());

        // enable to verify yml
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName + "/yml/stop"), userForUser))
            .andExpect(status().isOk());

        // enable to crate flow
        this.mockMvc.perform(requestWithUser(post("/flows/" + "newFlowName"), userForUser))
            .andExpect(status().isOk());

        // unable to delete flow
        this.mockMvc.perform(requestWithUser(delete("/flows/" + flowName), userForUser))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void should_admin_role_access_everything() throws Throwable {
        userForAdmin.setPassword(password);

        // enable to crate flow
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName), userForAdmin))
            .andExpect(status().isOk());

        // test to list flows
        this.mockMvc.perform(requestWithUser(get("/flows"), userForAdmin))
            .andExpect(status().isOk());

        // enable to verify yml
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName + "/yml/stop"), userForAdmin))
            .andExpect(status().isOk());

        // enable to delete flow
        this.mockMvc.perform(requestWithUser(delete("/flows/" + flowName), userForAdmin))
            .andExpect(status().isOk());
    }

    @After
    public void after() {
        authInterceptor.disable();
    }

    private MockHttpServletRequestBuilder requestWithUser(MockHttpServletRequestBuilder builder, User user) {
        LoginResponse login = userService.login(user.getEmail(), user.getPassword());
        String token = login.getToken();
        return builder.header(AuthenticationInterceptor.TOKEN_HEADER_PARAM, token);
    }
}
