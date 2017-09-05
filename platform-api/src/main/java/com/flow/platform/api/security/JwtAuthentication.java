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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author yang
 */
public class JwtAuthentication implements Authentication {

    /**
     * User related roles
     */
    private final List<GrantedAuthority> roles = new LinkedList<>();

    /**
     * User email
     */
    private final String email;

    public JwtAuthentication(String email) {
        this.email = email;
    }

    public JwtAuthentication(String email, String... roles) {
        this.email = email;
        for (String role : roles) {
            this.roles.add(new SimpleGrantedAuthority(role));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public Object getCredentials() {
        return "cert";
    }

    @Override
    public Object getDetails() {
        return "N/A";
    }

    @Override
    public Object getPrincipal() {
        return "principal";
    }

    @Override
    public boolean isAuthenticated() {
        // set to false since the detail user info will be extracted from JwtAuthProvider
        return false;
    }

    @Override
    public void setAuthenticated(boolean b) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String getName() {
        return email;
    }
}
