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

import com.flow.platform.api.domain.request.ActionParam;
import com.flow.platform.api.domain.user.ActionGroup;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalStatusException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class ActionServiceTest extends TestBase {

    @Autowired
    private ActionService actionService;

    @Autowired
    private RoleService roleService;

    @Test
    public void should_create_action() {
        Action action = new Action();
        action.setName("test");
        action.setAlias("test-super-admin");
        action.setTag(ActionGroup.SUPERADMIN);
        actionService.create(action);

        Assert.assertEquals(1, actionService.list().size());
    }

    @Test
    public void should_update_action() {
        // given:
        final String actionName = "test";

        Action action = new Action();
        action.setName(actionName);
        action.setTag(ActionGroup.SUPERADMIN);
        actionService.create(action);

        // when:
        actionService.update(actionName, new ActionParam("test-test", "desc", ActionGroup.DEFAULT));

        // then:
        Action loaded = actionService.find(actionName);
        Assert.assertEquals(ActionGroup.DEFAULT, loaded.getTag());
        Assert.assertEquals("test-test", loaded.getAlias());
    }

    @Test
    public void should_delete_action() {
        Action action = new Action();
        action.setName("test");
        action.setTag(ActionGroup.MANAGER);
        actionService.create(action);

        actionService.delete("test");
        Assert.assertEquals(0, actionService.list().size());
    }

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_if_has_role_assigned_to_action() {
        // given: action
        Action action = new Action();
        action.setName("test-with-role");
        action.setTag(ActionGroup.MANAGER);
        actionService.create(action);

        // given: role
        Role admin = roleService.create("admin", null);

        // given: assign role to action
        permissionDao.save(new Permission(admin.getId(), action.getName()));

        // then:
        actionService.delete(action.getName());
    }

    @Test
    public void should_list_roles() {
        Action action = new Action();
        action.setName("test");
        action.setTag(ActionGroup.MANAGER);
        actionService.create(action);

        Assert.assertEquals(1, actionService.list().size());
    }
}
