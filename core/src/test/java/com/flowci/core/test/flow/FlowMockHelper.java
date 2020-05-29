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
import com.flowci.core.flow.domain.Flow;
import com.flowci.domain.VarValue;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.user.domain.User;
import com.flowci.domain.http.RequestMessage;
import com.flowci.domain.http.ResponseMessage;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * @author yang
 */
public class FlowMockHelper {

    static final TypeReference<ResponseMessage<Flow>> FlowType =
            new TypeReference<ResponseMessage<Flow>>() {
            };

    static final TypeReference<ResponseMessage<List<Flow>>> ListFlowType =
            new TypeReference<ResponseMessage<List<Flow>>>() {
            };

    static final TypeReference<ResponseMessage<String>> FlowYmlType =
            new TypeReference<ResponseMessage<String>>() {
            };

    private static final TypeReference<ResponseMessage<List<User>>> UserListType =
            new TypeReference<ResponseMessage<List<User>>>() {
            };

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    public Flow create(String name, String yml) throws Exception {
        // create
        ResponseMessage<Flow> response = mockMvcHelper
                .expectSuccessAndReturnClass(post("/flows/" + name), FlowType);

        Assert.assertEquals(StatusCode.OK, response.getCode());

        // confirm
        response = mockMvcHelper.expectSuccessAndReturnClass(post("/flows/" + name + "/confirm"), FlowType);
        Assert.assertEquals(StatusCode.OK, response.getCode());

        // save yml
        String base64Encoded = Base64.getEncoder().encodeToString(yml.getBytes());
        RequestMessage<String> message = new RequestMessage<>(base64Encoded);

        mockMvcHelper.expectSuccessAndReturnString(
                post("/flows/" + name + "/yml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(message))
        );

        return response.getData();
    }

    void addUsers(String name, User... users) throws Exception {
        ResponseMessage<?> message = mockMvcHelper.expectSuccessAndReturnClass(
                post("/flows/" + name + "/users")
                        .content(objectMapper.writeValueAsBytes(toUserEmailList(users)))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
        Assert.assertEquals(StatusCode.OK, message.getCode());
    }

    List<User> listUsers(String name) throws Exception {
        ResponseMessage<List<User>> message = mockMvcHelper.expectSuccessAndReturnClass(
                get("/flows/" + name + "/users"), UserListType);

        Assert.assertEquals(StatusCode.OK, message.getCode());
        return message.getData();
    }

    void removeUsers(String name, User... users) throws Exception {
        ResponseMessage<?> message = mockMvcHelper.expectSuccessAndReturnClass(
                delete("/flows/" + name + "/users")
                        .content(objectMapper.writeValueAsBytes(toUserEmailList(users)))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);
        Assert.assertEquals(StatusCode.OK, message.getCode());
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
