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
package com.flow.platform.api.domain.credential;

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;

/**
 * @author lhl
 */
public class Credential extends CreateUpdateObject {

    @Expose
    protected String name;

    @Expose
    protected CredentialType type;

    @Expose
    protected String createdBy;

    /**
     * The credential detail will be saved as raw json
     */
    @Expose
    private CredentialDetail detail;

    public Credential() {
    }

    public Credential(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public CredentialDetail getDetail() {
        return detail;
    }

    public void setDetail(CredentialDetail detail) {
        this.detail = detail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Credential credential = (Credential) o;

        return name.equals(credential.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Credential{" +
            "name='" + name + '\'' +
            ", type=" + type +
            '}';
    }
}
