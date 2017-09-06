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

package com.flow.platform.api.security;

import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.exception.AccessDeniedException;
import com.flow.platform.api.exception.AuthenticationException;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author yang
 */
public class AuthenticationInterceptor extends HandlerInterceptorAdapter {

    public final static String TOKEN_HEADER_PARAM = "X-Authorization";

    private final static Logger LOGGER = new Logger(AuthenticationInterceptor.class);

    private final static String TOKEN_PAYLOAD_FOR_TEST = "mytokenpayload";

    private final static boolean ENABLE_AUTH = Boolean.parseBoolean(System.getProperty("auth.enable", "false"));

    @Autowired
    private UserSecurityService userSecurityService;

    @Autowired
    private TokenGenerator tokenGenerator;

    /**
     * Requests needs to verify token
     */
    private final List<RequestMatcher> authRequests;

    public AuthenticationInterceptor(List<RequestMatcher> matchers) {
        this.authRequests = matchers;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!ENABLE_AUTH) {
            return true;
        }

        if(!isNeedToVerify(request)) {
            return true;
        }

        try {
            doVerify(request, handler);
            return true;
        } catch (Throwable e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), e.getMessage());
            return false;
        }
    }

    private void doVerify(HttpServletRequest request, Object handler) {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (handler == null) {
            return;
        }

        // check token is provided from http header
        String tokenPayload = request.getHeader(TOKEN_HEADER_PARAM);
        if (Strings.isNullOrEmpty(tokenPayload)) {
            throw new AuthenticationException("Invalid request");
        }

        if (TOKEN_PAYLOAD_FOR_TEST.equals(tokenPayload)) {
            return;
        }

        // get user email from token payload
        String email = tokenGenerator.extract(tokenPayload);

        // find annotation
        WebSecurity securityAnnotation = handlerMethod.getMethodAnnotation(WebSecurity.class);
        if (securityAnnotation == null) {
            return;
        }

        // find action for request
        Action action = userSecurityService.getAction(securityAnnotation.action());
        LOGGER.debug("User '%s' requested for action %s", email, action.getName());

        if (!userSecurityService.canAccess(email, action)) {
            throw new AccessDeniedException(email, action.getName());
        }
    }

    private boolean isNeedToVerify(HttpServletRequest request) {
        for (RequestMatcher matcher : authRequests) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }
}
