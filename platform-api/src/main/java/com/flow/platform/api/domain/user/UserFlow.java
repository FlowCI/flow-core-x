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
public class UserFlow extends CreateUpdateObject {

    @Expose
    private UserFlowKey key;

    @Expose
    private String createdBy;

    public UserFlowKey getKey() {
        return key;
    }

    public void setKey(UserFlowKey key) {
        this.key = key;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UserFlow() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserFlow userFlow = (UserFlow) o;

        if (key != null ? !key.equals(userFlow.key) : userFlow.key != null) {
            return false;
        }
        return createdBy != null ? createdBy.equals(userFlow.createdBy) : userFlow.createdBy == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserFlow{" +
            "key=" + key +
            ", createdBy='" + createdBy + '\'' +
            '}';
    }
}
