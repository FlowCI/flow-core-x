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

package com.flowci.core.auth.controller;

import com.flowci.common.exception.AccessException;
import com.flowci.common.exception.AuthenticationException;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.auth.annotation.Action;
import com.flowci.core.auth.service.AuthService;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.domain.User;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;
import java.util.Optional;

/**
 * @author yang
 */
@Component("webAuth")
@AllArgsConstructor
public class WebAuth implements HandlerInterceptor {

    private static final String HeaderToken = "Token";

    private static final String ParameterToken = "token";

    private final AuthService authService;

    private final SessionManager sessionManager;

    /**
     * Get user object from ws message header
     *
     * @param headers
     * @return User object
     * @throws AuthenticationException if Token header is missing or invalid token
     */
    public User validate(MessageHeaders headers) {
        MultiValueMap<String, String> map = headers.get(StompHeaderAccessor.NATIVE_HEADERS, MultiValueMap.class);
        if (Objects.isNull(map)) {
            throw new AuthenticationException("Invalid token");
        }

        String token = map.getFirst(HeaderToken);
        if (StringHelper.isEmpty(token)) {
            throw new AuthenticationException("Invalid token");
        }

        Optional<User> user = authService.get(token);
        if (user.isEmpty()) {
            throw new AuthenticationException("Invalid token");
        }

        return user.get();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!authService.isEnabled()) {

            // do not update existing user
            if (sessionManager.exist()) {
                return true;
            }

            return true;
        }

        String token = getToken(request);
        if (!authService.set(token)) {
            throw new AuthenticationException("Invalid token");
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Action action = handlerMethod.getMethodAnnotation(Action.class);

        if (!authService.hasPermission(action)) {
            throw new AccessException("No permission");
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {

        if (authService.isEnabled()) {
            sessionManager.remove();
        }
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HeaderToken);

        if (Strings.isNullOrEmpty(token)) {
            token = request.getParameter(ParameterToken);
        }

        if (Strings.isNullOrEmpty(token)) {
            throw new AuthenticationException("Token is missing");
        }

        return token;
    }
}
