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

package com.flowci.core.test.flow;

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUsersDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.test.SpringScenario;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

public class FlowUserListDaoTest extends SpringScenario {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private FlowUsersDao flowUsersDao;

    @Test
    public void should_insert_unique_user_id() {
        Flow flow = createFlow("test-flow");

        HashSet<String> userIds = Sets.newHashSet("user1", "user2");
        flowUsersDao.insert(flow.getId(), userIds);

        HashSet<String> userDuplicate = Sets.newHashSet("user2");
        flowUsersDao.insert(flow.getId(), userDuplicate);

        var optional = flowUsersDao.findById(flow.getId());
        Assert.assertTrue(optional.isPresent());

        List<String> users = optional.get().getUsers();
        Assert.assertEquals(2, users.size());
    }

    @Test
    public void should_remove_users() {
        Flow flow = createFlow("test-flow");

        HashSet<String> userIds = Sets.newHashSet("1", "2");
        flowUsersDao.insert(flow.getId(), userIds);

        List<String> users = flowUsersDao.findById(flow.getId()).get().getUsers();
        Assert.assertEquals(2, users.size());

        // when: remove user1
        flowUsersDao.remove(flow.getId(), Sets.newHashSet("1"));
        users = flowUsersDao.findById(flow.getId()).get().getUsers();
        Assert.assertEquals(1, users.size());

        // then: check existing
        Assert.assertFalse(flowUsersDao.exist(flow.getId(), "1"));
        Assert.assertTrue(flowUsersDao.exist(flow.getId(), "2"));
    }

    @Test
    public void should_list_flow_ids_by_user() {
        Flow flow1 = createFlow("test-flow-1");
        Flow flow2 = createFlow("test-flow-2");
        Flow flow3 = createFlow("test-flow-3");

        // when: set user1 for flow 1,2,3, user2 for flow 1
        flowUsersDao.insert(flow1.getId(), Sets.newHashSet("1", "2"));
        flowUsersDao.insert(flow2.getId(), Sets.newHashSet("1"));
        flowUsersDao.insert(flow3.getId(), Sets.newHashSet("1"));

        // then:
        List<String> flowsForUser1 = flowUsersDao.findAllFlowsByUserEmail("1");
        Assert.assertEquals(3, flowsForUser1.size());

        List<String> flowsForUser2 = flowUsersDao.findAllFlowsByUserEmail("2");
        Assert.assertEquals(1, flowsForUser2.size());
    }

    private Flow createFlow(String name) {
        Flow flow = new Flow();
        flow.setName(name);
        flowDao.insert(flow);
        return flow;
    }
}
