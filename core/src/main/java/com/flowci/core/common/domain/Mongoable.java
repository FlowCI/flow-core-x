/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.common.domain;

import com.flowci.common.helper.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yang
 */
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id"})
@Getter
@Setter
public abstract class Mongoable implements Serializable {

    public static final Sort SortByCreatedAtASC = Sort.by(Sort.Direction.ASC, "createdAt");

    @Id
    protected String id;

    @CreatedDate
    protected Date createdAt;

    /**
     * User email
     */
    @CreatedBy
    protected String createdBy;

    @LastModifiedDate
    protected Date updatedAt;

    /**
     * User email
     */
    @LastModifiedBy
    protected String updatedBy;

    public boolean hasId() {
        return StringHelper.hasValue(id);
    }

    public void cleanDBInfo() {
        this.id = null;
        this.createdAt = null;
        this.updatedAt = null;
        this.createdBy = null;
        this.updatedBy = null;
    }

    public void setCreatedAndUpdatedBy(String email) {
        updatedBy = email;
        if (!StringHelper.hasValue(id)) {
            createdBy = email;
        }
    }
}
