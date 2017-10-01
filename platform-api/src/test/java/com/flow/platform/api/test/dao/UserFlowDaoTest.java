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

import com.flow.platform.api.domain.user.UserFlow;
import com.flow.platform.api.domain.user.UserFlowKey;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author lhl
 */
public class UserFlowDaoTest extends TestBase {

    @Test
    public void should_create_user_flow() {
        UserFlow userFlow = new UserFlow(new UserFlowKey("flow-integration", "liuhailiang@126.com"));
        userFlowDao.save(userFlow);

        Assert.assertEquals("liuhailiang@126.com", userFlow.getEmail());
        Assert.assertEquals(1, userFlowDao.list().size());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void should_raise_exception_if_create_duplicate_user_flow() {
        UserFlow userFlow = new UserFlow(new UserFlowKey("flow-integration", "liuhailiang@126.com"));
        userFlowDao.save(userFlow);

        userFlow = new UserFlow(new UserFlowKey("flow-integration", "liuhailiang@126.com"));
        userFlowDao.save(userFlow);
    }

    @Test
    public void should_find_user_flow_by_email_and_delete() {
        final String email = "liuhailiang@126.com";
        final String flowPath = "flow-integration";

        // then: create two user-flow for the same email
        UserFlowKey userFlowKey = new UserFlowKey(flowPath, email);
        UserFlow userFlow = new UserFlow(userFlowKey);
        userFlowDao.save(userFlow);

        UserFlowKey userFlowKeyAnother = new UserFlowKey("flow-2", email);
        UserFlow userFlowAnother = new UserFlow(userFlowKeyAnother);
        userFlowDao.save(userFlowAnother);

        Assert.assertEquals(2, userFlowDao.listByEmail(email).size());

        // then: delete user-flow by email
        int numOfRows = userFlowDao.deleteByEmail(email);
        Assert.assertEquals(2, numOfRows);
        Assert.assertEquals(0, userFlowDao.listByEmail(email).size());
    }

    @Test
    public void should_delete_user_flow() {
        UserFlowKey userFlowKey = new UserFlowKey("flow-integration", "liuhailiang@126.com");
        UserFlow userFlow = new UserFlow(userFlowKey);
        userFlowDao.save(userFlow);

        userFlowDao.delete(userFlow);
        Assert.assertEquals(0, userFlowDao.list().size());
    }

    @Test
    public void should_get_number_of_user_per_flow() {
        final String flowPath = "flow-integration";
        UserFlowKey userFlowKey = new UserFlowKey(flowPath, "liuhailiang@126.com");
        UserFlow userFlow = new UserFlow(userFlowKey);
        userFlowDao.save(userFlow);

        userFlowKey = new UserFlowKey(flowPath, "hello@126.com");
        userFlow = new UserFlow(userFlowKey);
        userFlowDao.save(userFlow);

        Assert.assertEquals(2, userFlowDao.numOfUser(flowPath).longValue());
    }
}
