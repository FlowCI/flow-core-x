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
public class PermissionKey implements Serializable {

    @Expose
    private Integer roleId;

    @Expose
    private String action;

    public PermissionKey() {
    }

    public PermissionKey(Integer roleId, String action) {
        this.roleId = roleId;
        this.action = action;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionKey that = (PermissionKey) o;

        if (!roleId.equals(that.roleId)) {
            return false;
        }
        return action.equals(that.action);
    }

    @Override
    public int hashCode() {
        int result = roleId.hashCode();
        result = 31 * result + action.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PermissionKey{" +
            "roleId=" + roleId +
            ", action='" + action + '\'' +
            '}';
    }
}
