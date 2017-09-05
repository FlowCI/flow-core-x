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

import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.util.ExceptionUtil;
import com.google.common.base.Strings;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Filter to get JWT token from header
 *
 * @author yang
 */
public class JwtAuthFilter extends AbstractAuthenticationProcessingFilter {

    private final static String JWT_TOKEN_HEADER_PARAM = "X-Authorization";

    private final static String TOKEN_PAYLOAD_FOR_TEST = "mytokenpayload";

    private final TokenGenerator tokenGenerator;

    public JwtAuthFilter(RequestMatcher matcher, TokenGenerator tokenGenerator) {
        super(matcher);
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        String tokenPayload = request.getHeader(JWT_TOKEN_HEADER_PARAM);

        if (Strings.isNullOrEmpty(tokenPayload)) {
            throw new BadCredentialsException("Invalid request");
        }

        if (TOKEN_PAYLOAD_FOR_TEST.equals(tokenPayload)) {
            return new JwtAuthentication("admin@flow.ci", "ROLE_ADMIN", "ROLE_MANAGER");
        }

        try {
            String email = tokenGenerator.extract(tokenPayload);
            return new JwtAuthentication(email);
        } catch (Throwable e) {
            throw new BadCredentialsException(ExceptionUtil.findRootCause(e).getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null) {
            context = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(context);
        }

        context.setAuthentication(authResult);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        throw failed;
    }
}
