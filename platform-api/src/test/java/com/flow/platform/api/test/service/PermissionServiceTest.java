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
package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.test.TestBase;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class PermissionServiceTest extends TestBase {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ActionService actionService;

    @Autowired
    private RoleService roleService;

    @Test
    public void should_permission_assigned() {
        // given:
        Role role = createMockRole("Admin");
        Action actionForShow = createMockAction("Show");
        Action actionForCreate = createMockAction("Create");

        // when: assign actions to role
        permissionService.assign(role, Sets.newHashSet(actionForShow, actionForCreate));

        // then:
        Assert.assertEquals(2, permissionService.list(role).size());
        Assert.assertEquals(1, permissionService.list(actionForShow).size());
        Assert.assertEquals(1, permissionService.list(actionForCreate).size());

        // when: un-assign actions from role
        permissionService.unAssign(role, Sets.newHashSet(actionForCreate, actionForShow));

        // then:
        Assert.assertEquals(0, permissionService.list(role).size());
        Assert.assertEquals(0, permissionService.list(actionForShow).size());
        Assert.assertEquals(0, permissionService.list(actionForCreate).size());
    }

    private Action createMockAction(String name) {
        Action action = new Action(name, name, "desc...");
        return actionService.create(action);
    }

    private Role createMockRole(String name) {
        return roleService.create(name, null);
    }
}
