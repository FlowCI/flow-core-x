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

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.api.test.TestBase;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class UserFlowServiceTest extends TestBase {

    @Autowired
    private UserFlowService userFlowService;

    @Test
    public void should_assign_user_flow(){

        final String flowPath = "flow-integration";
        final String email = "test@fir.im";

        User user = new User(email, "test", "12345");
        userDao.save(user);
        Assert.assertNotNull(userDao.get(user.getEmail()));

        Flow flow = nodeService.createEmptyFlow(flowPath);
        Assert.assertNotNull(nodeService.find(flowPath));

        // when: assign user to role
        userFlowService.assign(user, flow);

        // then:
        List<User> usersForFlow = userFlowService.list(flowPath);
        Assert.assertEquals(1, usersForFlow.size());
        Assert.assertEquals(user, usersForFlow.get(0));

        // when: un-assign user to role
        userFlowService.unAssign(user, flow);

        // then:
        usersForFlow = userFlowService.list(flowPath);
        Assert.assertEquals(0, usersForFlow.size());
    }
}
