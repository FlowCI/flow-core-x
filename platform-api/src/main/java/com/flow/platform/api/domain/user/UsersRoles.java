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
package com.flow.platform.api.domain.user;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;

/**
 * @author lhl
 */
public class UsersRoles extends Jsonable {

    @Expose
    private UserRoleKey userRoleKey;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    public UserRoleKey getUserRoleKey() {
        return userRoleKey;
    }

    public void setUserRoleKey(UserRoleKey userRoleKey) {
        this.userRoleKey = userRoleKey;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UsersRoles() {
    }

    public UsersRoles(Integer roleId, String email) {
        this(new UserRoleKey(roleId, email));
    }

    public UsersRoles(UserRoleKey userRoleKey) {
        this.userRoleKey = userRoleKey;
    }


    public Integer getRoleId() {
        return this.userRoleKey.getRoleId();
    }

    public String getEmail() {
        return this.userRoleKey.getEmail();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UsersRoles that = (UsersRoles) o;

        return userRoleKey.equals(that.userRoleKey);
    }

    @Override
    public int hashCode() {
        return userRoleKey.hashCode();
    }

    @Override
    public String toString() {
        return "UsersRoles{" +
            "userRoleKey=" + userRoleKey +
            '}';
    }
}
