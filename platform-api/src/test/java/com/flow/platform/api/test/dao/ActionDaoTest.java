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
package com.flow.platform.api.test.dao;

import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.ActionGroup;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author lhl
 */
public class ActionDaoTest extends TestBase {

    @Test
    public void should_create_permission() {
        // when:
        Action action = new Action();
        action.setName("test");
        action.setAlias("test");
        action.setDescription("test desc");
        action.setTag(ActionGroup.ADMIN);
        actionDao.save(action);

        // then:
        Action loaded = actionDao.get("test");
        Assert.assertNotNull(loaded);
        Assert.assertEquals("test", loaded.getName());
        Assert.assertEquals("test desc", loaded.getDescription());
        Assert.assertEquals(1, actionDao.list().size());
    }

    @Test
    public void should_update_permission_success() {
        // given:
        Action action = new Action();
        action.setName("test");
        action.setAlias("test");
        action.setDescription("test desc");
        action.setTag(ActionGroup.ADMIN);
        actionDao.save(action);

        // when:
        action.setAlias("test - admin");
        action.setTag(ActionGroup.SUPERADMIN);
        actionDao.update(action);

        // then:
        Action loaded = actionDao.get("test");
        Assert.assertEquals("test - admin", loaded.getAlias());
        Assert.assertEquals(ActionGroup.SUPERADMIN, loaded.getTag());
    }

    @Test
    public void should_delete_permission_success() {
        Action action = new Action();
        action.setName("test");
        action.setAlias("test-admin");
        action.setDescription("test desc");
        action.setTag(ActionGroup.ADMIN);

        actionDao.save(action);

        actionDao.delete(action);
        Assert.assertEquals(0, actionDao.list().size());
    }

}
