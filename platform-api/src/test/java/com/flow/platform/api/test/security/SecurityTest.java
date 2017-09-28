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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.AuthenticationInterceptor;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.test.TestBase;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
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
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ActionService actionService;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private AuthenticationInterceptor authInterceptor;


    private User userForAdmin;

    private User userForUser;

    private User userWithoutAuthority;

    private final String flowName = "flow1";

    @Before
    public void init() throws Throwable {
        authInterceptor.enable();

        // init two roles admin and user
        Role admin = new Role("ROLE_ADMIN", null);
        roleDao.save(admin);
        Set<Action> adminActions = new HashSet<>();

        Role user = new Role("ROLE_USER", null);
        roleDao.save(user);

        Role ymlOperator = new Role("ROLE_YML", null);
        roleDao.save(ymlOperator);

        // init all defined actions
        for(Actions item : Actions.values()) {
            Action action = actionService.create(new Action(item.name()));
            adminActions.add(action);
        }

        // assign actions to admin,user and yml role
        permissionService.assign(admin, adminActions);
        permissionService.assign(user, Sets.newHashSet(actionService.find(Actions.FLOW_SHOW.name())));
        permissionService.assign(user, Sets.newHashSet(actionService.find(Actions.FLOW_YML.name())));
        permissionService.assign(ymlOperator, Sets.newHashSet(actionService.find(Actions.FLOW_YML.name())));

        // init mock user
        userForAdmin = userService.register(new User("test1@flow.ci", "test1", "12345"),
                                            Lists.newArrayList("ROLE_ADMIN"), false,
                                            Lists.newArrayList("flow2"));

        userForUser = userService.register(new User("test2@flow.ci", "test2", "12345"),
                                            Lists.newArrayList("ROLE_USER"), false,
                                            Lists.newArrayList(flowName));
        userWithoutAuthority = userService.register(new User("test3@flow.ci", "test3", "12345"),
            Lists.newArrayList("ROLE_YML"), false,
            Lists.newArrayList(flowName));

    }

    @Test
    public void should_raise_401_when_access_url_from_user_without_role() throws Throwable {
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
        createRootFlow(flowName, "demo_flow.yaml");

        // test to list flows
        this.mockMvc.perform(requestWithUser(get("/flows"), userForUser))
            .andExpect(status().isOk());

        // enable to verify yml
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName + "/yml/stop"), userForUser))
            .andExpect(status().isOk());

        // unable to crate flow
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName), userForUser))
            .andExpect(status().isUnauthorized());

        // unable to delete flow
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName + "/delete"), userForUser))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void should_admin_role_access_everything() throws Throwable {
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
        this.mockMvc.perform(requestWithUser(post("/flows/" + flowName + "/delete"), userForAdmin))
            .andExpect(status().isOk());
    }

    @After
    public void after() {
        authInterceptor.disable();
    }

    private MockHttpServletRequestBuilder requestWithUser(MockHttpServletRequestBuilder builder, User user) {
        String token = tokenGenerator.create(user.getEmail(), 100);
        return builder.header(AuthenticationInterceptor.TOKEN_HEADER_PARAM, token);
    }

}
