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

package com.flowci.core.test.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentOption;
import com.flowci.core.agent.domain.DeleteAgent;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.common.exception.ErrorCode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * @author yang
 */
public class AgentControllerTest extends MockLoggedInScenario {

    private static final TypeReference<ResponseMessage<Agent>> AgentResponseType =
            new TypeReference<ResponseMessage<Agent>>() {
            };

    private static final TypeReference<ResponseMessage<List<Agent>>> AgentListResponseType =
            new TypeReference<ResponseMessage<List<Agent>>>() {
            };

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void should_duplicate_error_when_create_agent_with_same_name() throws Throwable {
        createAgent("same.name", null, StatusCode.OK);
        createAgent("same.name", null, ErrorCode.DUPLICATE);
    }

    @Test
    public void should_list_agent() throws Throwable {
        Agent first = createAgent("first.agent", null, StatusCode.OK);
        Agent second = createAgent("second.agent", null, StatusCode.OK);

        ResponseMessage<List<Agent>> response =
                mockMvcHelper.expectSuccessAndReturnClass(get("/agents"), AgentListResponseType);
        Assert.assertEquals(StatusCode.OK, response.getCode());

        List<Agent> list = response.getData();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(first));
        Assert.assertTrue(list.contains(second));
    }

    @Test
    public void should_delete_agent() throws Throwable {
        Agent created = createAgent("should.delete", null, StatusCode.OK);

        DeleteAgent body = new DeleteAgent(created.getToken());
        ResponseMessage<Agent> responseOfDeleteAgent =
                mockMvcHelper.expectSuccessAndReturnClass(
                        delete("/agents")
                                .content(objectMapper.writeValueAsString(body))
                                .contentType(MediaType.APPLICATION_JSON), AgentResponseType);

        Assert.assertEquals(StatusCode.OK, responseOfDeleteAgent.getCode());
        Assert.assertEquals(created, responseOfDeleteAgent.getData());

        ResponseMessage<Agent> responseOfGetAgent =
                mockMvcHelper.expectSuccessAndReturnClass(get("/agents/" + created.getToken()), AgentResponseType);
        Assert.assertEquals(ErrorCode.NOT_FOUND, responseOfGetAgent.getCode());
    }

    private Agent createAgent(String name, Set<String> tags, Integer code) throws Exception {
        AgentOption create = new AgentOption();
        create.setName(name);
        create.setTags(tags);

        ResponseMessage<Agent> agentR = mockMvcHelper.expectSuccessAndReturnClass(post("/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(create)), AgentResponseType);

        Assert.assertEquals(code, agentR.getCode());
        return agentR.getData();
    }
}