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
public class UserFlowKey implements Serializable {

    @Expose
    private String flowPath;

    @Expose
    private String email;

    public String getFlowPath() {
        return flowPath;
    }

    public void setFlowPath(String flowPath) {
        this.flowPath = flowPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserFlowKey(String flowPath, String email) {
        this.flowPath = flowPath;
        this.email = email;
    }

    public UserFlowKey() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserFlowKey that = (UserFlowKey) o;

        if (flowPath != null ? !flowPath.equals(that.flowPath) : that.flowPath != null) {
            return false;
        }
        return email != null ? email.equals(that.email) : that.email == null;
    }

    @Override
    public int hashCode() {
        int result = flowPath != null ? flowPath.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserFlowKey{" +
            "flowPath='" + flowPath + '\'' +
            ", email='" + email + '\'' +
            '}';
    }
}
