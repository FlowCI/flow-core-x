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

package com.flowci.core.test.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.auth.domain.Tokens;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.domain.http.ResponseMessage;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class AuthHelper {

    private final TypeReference<ResponseMessage<Tokens>> tokensType =
        new TypeReference<ResponseMessage<Tokens>>() {
        };

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private AppProperties.Auth authProperties;

    @Autowired
    private ObjectMapper objectMapper;

    public void enableAuth() {
        authProperties.setEnabled(true);
    }

    public void disableAuth() {
        authProperties.setEnabled(false);
    }

    public ResponseMessage<Tokens> login(String email, String passwordOnMd5) throws Exception {
        String authContent = email + ":" + passwordOnMd5;
        String base64Content = Base64.getEncoder().encodeToString(authContent.getBytes());
        MockHttpServletRequestBuilder builder = post("/auth/login").header("Authorization", "Basic " + base64Content);

        return mockMvcHelper.expectSuccessAndReturnClass(builder, tokensType);
    }

    public ResponseMessage<Tokens> refresh(Tokens tokens) throws Exception {
        return mockMvcHelper.expectSuccessAndReturnClass(
            post("/auth/refresh")
                .content(objectMapper.writeValueAsString(tokens))
                .contentType(MediaType.APPLICATION_JSON),
            tokensType
        );
    }

    public ResponseMessage logout(String token) throws Exception {
        return mockMvcHelper.expectSuccessAndReturnClass(
            post("/auth/logout").header("Token", token),
            ResponseMessage.class
        );
    }
}
