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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.user.domain.User;
import com.flowci.common.domain.VarValue;
import com.flowci.common.helper.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * @author yang
 */
public class FlowMockHelper {

    static final TypeReference<ResponseMessage<Flow>> FlowType = new TypeReference<>() {
    };

    static final TypeReference<ResponseMessage<List<Flow>>> ListFlowType = new TypeReference<>() {
    };

    static final TypeReference<ResponseMessage<FlowYml>> FlowYmlType = new TypeReference<>() {
    };

    static final TypeReference<ResponseMessage<String>> FlowYmlContentType = new TypeReference<>() {
    };

    private static final TypeReference<ResponseMessage<List<User>>> UserListType = new TypeReference<>() {
    };

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    public Flow create(String name, String yml) throws Exception {
        var options = new CreateOption().setRawYaml(StringHelper.toBase64(yml));

        var post = post("/flows/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(options));

        return mockMvcHelper.expectSuccessAndReturnClass(post, FlowType).getData();
    }

    void addUsers(String name, User... users) throws Exception {
        ResponseMessage<?> message = mockMvcHelper.expectSuccessAndReturnClass(
                post("/flows/" + name + "/users")
                        .content(objectMapper.writeValueAsBytes(toUserEmailList(users)))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
        assertEquals(StatusCode.OK, message.getCode());
    }

    List<User> listUsers(String name) throws Exception {
        ResponseMessage<List<User>> message = mockMvcHelper.expectSuccessAndReturnClass(
                get("/flows/" + name + "/users"), UserListType);

        assertEquals(StatusCode.OK, message.getCode());
        return message.getData();
    }

    void removeUsers(String name, User... users) throws Exception {
        ResponseMessage<?> message = mockMvcHelper.expectSuccessAndReturnClass(
                delete("/flows/" + name + "/users")
                        .content(objectMapper.writeValueAsBytes(toUserEmailList(users)))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
        assertEquals(StatusCode.OK, message.getCode());
    }

    ResponseMessage addVars(String name, Map<String, VarValue> vars) throws Exception {
        return mockMvcHelper.expectSuccessAndReturnClass(
                post("/flows/" + name + "/variables")
                        .content(objectMapper.writeValueAsBytes(vars))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
    }

    ResponseMessage removeVars(String name, List<String> vars) throws Exception {
        return mockMvcHelper.expectSuccessAndReturnClass(
                delete("/flows/" + name + "/variables")
                        .content(objectMapper.writeValueAsBytes(vars))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
    }

    private List<String> toUserEmailList(User... users) {
        List<String> ids = new ArrayList<>(users.length);
        for (User item : users) {
            ids.add(item.getEmail());
        }
        return ids;
    }
}
