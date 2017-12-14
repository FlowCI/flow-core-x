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

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.node.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author yang
 */
public class StepControllerTest extends ControllerTestWithoutAuth {

    private final String flowName = "flow_default";

    @Before
    public void init() throws Throwable {
        stubDemo();
        createEmptyFlow(flowName);
    }

    @Test
    public void should_list_children_node_of_root() throws Throwable {
        // given:
        createYml(flowName, "yml/demo_flow2.yaml");

        // when:
        MvcResult result = mockMvc.perform(get("/flows/" + flowName + "/steps")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        // then:
        Node[] children = Node.parseArray(result.getResponse().getContentAsString().getBytes(), Node[].class);
        Assert.assertNotNull(children);
        Assert.assertEquals(8, children.length);
    }
}
