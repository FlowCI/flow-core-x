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

import com.google.gson.annotations.Expose;
import java.io.Serializable;

/**
 * @author lhl
 */
public class RolePermissionKey implements Serializable {

    @Expose
    private Integer roleId;

    @Expose
    private String action;

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public RolePermissionKey(Integer roleId, String action) {
        this.roleId = roleId;
        this.action = action;
    }

    public RolePermissionKey() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RolePermissionKey that = (RolePermissionKey) o;

        if (roleId != null ? !roleId.equals(that.roleId) : that.roleId != null) {
            return false;
        }
        return action != null ? action.equals(that.action) : that.action == null;
    }

    @Override
    public int hashCode() {
        int result = roleId != null ? roleId.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RolePermissionKey{" +
            "roleId=" + roleId +
            ", action='" + action + '\'' +
            '}';
    }
}
