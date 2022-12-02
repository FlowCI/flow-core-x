/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.flow;

import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.user.domain.User;
import com.flowci.domain.VarType;
import com.flowci.domain.VarValue;
import com.flowci.exception.ErrorCode;
import com.flowci.util.StringHelper;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author yang
 */
public class FlowControllerTest extends MockLoggedInScenario {

    @Autowired
    private FlowMockHelper flowMockHelper;

    @Autowired
    private MockMvcHelper mockMvcHelper;

    private final String flowName = "hello_world";

    @Before
    public void createFlowWithYml() throws Exception {
        String yml = StringHelper.toString(load("flow.yml"));
        flowMockHelper.create(flowName, yml);
    }

    @Test
    public void should_get_flow_and_yml_then_delete() throws Exception {
        ResponseMessage<Flow> getFlowResponse = mockMvcHelper
                .expectSuccessAndReturnClass(get("/flows/" + flowName + "?group=false"), FlowMockHelper.FlowType);
        Assert.assertEquals(StatusCode.OK, getFlowResponse.getCode());
        Assert.assertEquals(flowName, getFlowResponse.getData().getName());

        ResponseMessage<List<FlowYml>> responseMessage = mockMvcHelper
                .expectSuccessAndReturnClass(get("/flows/" + flowName + "/yml"), FlowMockHelper.FlowYmlNameListType);
        List<FlowYml> yamlNameList = responseMessage.getData();
        Assert.assertEquals(1, yamlNameList.size());

        ResponseMessage<Flow> deleted = mockMvcHelper
                .expectSuccessAndReturnClass(delete("/flows/" + flowName), FlowMockHelper.FlowType);
        Assert.assertEquals(StatusCode.OK, deleted.getCode());
    }

    @Test
    public void should_list_flows() throws Exception {
        ResponseMessage<List<Flow>> listFlowResponse = mockMvcHelper
                .expectSuccessAndReturnClass(get("/flows"), FlowMockHelper.ListFlowType);

        Assert.assertEquals(StatusCode.OK, listFlowResponse.getCode());
        Assert.assertEquals(1, listFlowResponse.getData().size());
        Assert.assertEquals(flowName, listFlowResponse.getData().get(0).getName());
    }

    @Test
    public void should_list_users_of_flow() throws Exception {
        User user1 = userService.create("user.1@flow.ci", "12345", User.Role.Developer);
        User user2 = userService.create("user.2@flow.ci", "12345", User.Role.Developer);
        User user3 = userService.create("user.3@flow.ci", "12345", User.Role.Developer);

        flowMockHelper.addUsers(flowName, user1, user2, user3);

        List<User> users = flowMockHelper.listUsers(flowName);
        Assert.assertTrue(users.size() >= 3);

        flowMockHelper.removeUsers(flowName, user1, user2);
        users = flowMockHelper.listUsers(flowName);
        Assert.assertTrue(users.size() >= 1);
    }

    @Test
    public void should_operate_vars_to_flow() throws Exception {
        // init:
        Map<String, VarValue> vars = new HashMap<>();
        vars.put("FLOWCI_GIT_URL", VarValue.of("git@github.com:flowci/docs.git", VarType.GIT_URL));

        // to test add vars
        ResponseMessage msg = flowMockHelper.addVars(flowName, vars);
        Assert.assertEquals(StatusCode.OK, msg.getCode());

        // to test remove vars
        msg = flowMockHelper.removeVars(flowName, Lists.newArrayList("FLOWCI_GIT_URL"));
        Assert.assertEquals(StatusCode.OK, msg.getCode());
    }

    @Test
    public void should_get_argument_error_if_invalid_var_format() throws Exception {
        // init:
        Map<String, VarValue> vars = new HashMap<>();
        vars.put("FLOWCI_GIT_URL", VarValue.of("git@github.com", VarType.GIT_URL));

        ResponseMessage msg = flowMockHelper.addVars(flowName, vars);
        Assert.assertEquals(ErrorCode.INVALID_ARGUMENT, msg.getCode());
    }
}
