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

import static com.flow.platform.api.security.AuthenticationInterceptor.TOKEN_HEADER_PARAM;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.exception.AuthenticationException;
import com.flow.platform.api.service.user.UserService;
import com.google.common.base.Strings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author lhl
 */
public class OptionsInterceptor  extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers",
            "Origin, X-Requested-With, Content-Type, Accept, X-Authorization, Authenticate, Library");
        if (request.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return false;
        }

        return true;

    }

}
