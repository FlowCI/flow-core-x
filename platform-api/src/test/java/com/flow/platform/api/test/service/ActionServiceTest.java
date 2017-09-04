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

import com.flow.platform.api.domain.user.ActionGroup;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class ActionServiceTest extends TestBase {

    @Autowired
    private ActionService actionService;

    @Test
    public void should_create_permission() {
        Action action = new Action();
        action.setName("test");
        action.setAlias("test-super-admin");
        action.setTag(ActionGroup.SUPERADMIN);
        actionService.create(action);

        Assert.assertEquals(1, actionService.list().size());
    }

    @Test
    public void should_update_role() {
        // given:
        final String actionName = "test";

        Action action = new Action();
        action.setName(actionName);
        action.setTag(ActionGroup.SUPERADMIN);
        actionService.create(action);

        // when:
        action.setTag(ActionGroup.DEFAULT);
        action.setAlias("test-test");
        actionService.update(action);

        // then:
        Action loaded = actionService.find(actionName);
        Assert.assertEquals(ActionGroup.DEFAULT, loaded.getTag());
        Assert.assertEquals("test-test", loaded.getAlias());
    }

    @Test
    public void should_delete_role() {
        Action action = new Action();
        action.setName("test");
        action.setTag(ActionGroup.MANAGER);
        actionService.create(action);

        actionService.delete("test");
        Assert.assertEquals(0, actionService.list().size());
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
