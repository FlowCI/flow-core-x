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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Permission between role and action
 *
 * @author lhl
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"key"}, callSuper = false)
@ToString(of = {"key"})
public class Permission extends CreateUpdateObject {

    @Expose
    @Getter
    @Setter
    private PermissionKey key;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    public Permission(PermissionKey key) {
        this.key = key;
    }

    public Permission(Integer roleId, String action) {
        this(new PermissionKey(roleId, action));
    }

    public String getAction() {
        return key.getAction();
    }

    public Integer getRoleId() {
        return key.getRoleId();
    }
}
