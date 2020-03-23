/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.api.adviser;

import com.flowci.core.agent.service.AgentService;
import com.flowci.exception.AuthenticationException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * To verify token from agent
 */
@Component("apiAuth")
public class ApiAuth implements HandlerInterceptor {

    private static final String HeaderAgentToken = "AGENT-TOKEN";

    @Autowired
    private AgentService agentService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(HeaderAgentToken);

        if (StringHelper.hasValue(token) && agentService.isExisted(token)) {
            return true;
        }

        throw new AuthenticationException("Invalid token");
    }
}
