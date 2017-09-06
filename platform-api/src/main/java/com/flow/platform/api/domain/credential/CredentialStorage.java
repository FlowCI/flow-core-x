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

import com.flow.platform.domain.Jsonable;
import java.time.ZonedDateTime;

/**
 * @author lhl
 */
public class CredentialStorage extends Jsonable {

    private Credential content;

    private Integer id;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    public Credential getContent() {
        return content;
    }

    public void setContent(Credential content) {
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public CredentialStorage(Credential content, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CredentialStorage(){

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CredentialStorage that = (CredentialStorage) o;

        return  id != null ? !id.equals(that.id) : that.id != null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CredentialStorage{" +
            "id=" + id +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
