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
package com.flow.platform.api.config;

import com.flow.platform.api.security.JwtAuthFilter;
import com.flow.platform.api.security.JwtAuthProvider;
import com.flow.platform.api.security.token.JwtTokenGenerator;
import com.flow.platform.api.security.token.TokenGenerator;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


/**
 * @author lhl
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthProvider jwtAuthProvider;

    private final TokenGenerator tokenGenerator = new JwtTokenGenerator("MY_SECRET_KEY");

    @Bean
    public TokenGenerator tokenGenerator() {
        return tokenGenerator;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(jwtAuthProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        RequestMatcher matcher = buildRequestMatcher();

        JwtAuthFilter filter = new JwtAuthFilter(matcher, tokenGenerator);

        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .requestMatchers(matcher).hasRole("ADMIN");
    }

    private RequestMatcher buildRequestMatcher() {
        List<RequestMatcher> matcherList = new LinkedList<>();
        matcherList.add(new AntPathRequestMatcher("/flows/**"));
        return new OrRequestMatcher(matcherList);
    }
}

