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

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;

/**
 * @author lhl
 */
public class RolesPermissions extends CreateUpdateObject {

    @Expose
    private RolePermissionKey rolePermissionKey;

    public RolePermissionKey getRolePermissionKey() {
        return rolePermissionKey;
    }

    public RolesPermissions() {
    }

    public RolesPermissions(Integer roleId, String action) {
        this(new RolePermissionKey(roleId, action));
    }

    public void setRolePermissionKey(RolePermissionKey rolePermissionKey) {
        this.rolePermissionKey = rolePermissionKey;
    }

    public RolesPermissions(RolePermissionKey rolePermissionKey) {
        this.rolePermissionKey = rolePermissionKey;
    }


    public Integer getRoleId() {
        return this.rolePermissionKey.getRoleId();
    }

    public String getAction() {
        return this.rolePermissionKey.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RolesPermissions that = (RolesPermissions) o;

        return rolePermissionKey != null ? rolePermissionKey.equals(that.rolePermissionKey)
            : that.rolePermissionKey == null;
    }

    @Override
    public int hashCode() {
        return rolePermissionKey != null ? rolePermissionKey.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RolesPermissions{" +
            "rolePermissionKey=" + rolePermissionKey +
            '}';
    }
}
