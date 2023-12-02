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

import com.flowci.common.exception.AuthenticationException;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.agent.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * To verify token from agent
 */
@Component("apiAuth")
public class ApiAuth implements HandlerInterceptor {

    public static final String LocalTaskToken = "local-task";

    public static final String HeaderAgentToken = "AGENT-TOKEN";

    @Autowired
    private AgentService agentService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(HeaderAgentToken);

        if (StringHelper.hasValue(token)) {
            if (agentService.isExisted(token) || token.equals(LocalTaskToken)) {
                return true;
            }
        }

        throw new AuthenticationException("Invalid token");
    }
}
