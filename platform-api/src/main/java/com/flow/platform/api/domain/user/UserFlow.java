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
 * @author lhl
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserFlow extends CreateUpdateObject {

    @Expose
    @Getter
    @Setter
    private UserFlowKey key;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    public UserFlow(String flowPath, String email) {
        this(new UserFlowKey(flowPath, email));
    }

    public UserFlow(UserFlowKey userFlowKey) {
        this.key = userFlowKey;
    }

    public String getFlowPath() {
        return this.key.getFlowPath();
    }

    public String getEmail() {
        return this.key.getEmail();
    }

}
