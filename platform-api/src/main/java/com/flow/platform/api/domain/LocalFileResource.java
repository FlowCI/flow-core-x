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

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@firim
 */

@EqualsAndHashCode(of = {"id"}, callSuper = false)
@ToString(of = {"id", "name", "extension"})
public class LocalFileResource extends Jsonable {

    // file name
    @Expose
    @Getter
    @Setter
    private String name;

    // id
    @Expose
    @Getter
    @Setter
    private String id;

    // extend
    @Expose
    @Getter
    @Setter
    private String extension;

    @Expose
    @Getter
    @Setter
    private String url;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;


    public LocalFileResource() {
    }

    public LocalFileResource(String id) {
        this.id = id;
    }

    public LocalFileResource(String name, String id, String extension) {
        this.name = name;
        this.id = id;
        this.extension = extension;
    }

    public LocalFileResource(String name, String id, String extension, ZonedDateTime createdAt) {
        this.name = name;
        this.id = id;
        this.extension = extension;
        this.createdAt = createdAt;
    }
}
