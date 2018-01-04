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

/**
 * @author yh@firim
 */
public class LocalFileResource extends Jsonable{

    // file name
    @Expose
    private String name;

    // id
    @Expose
    private String id;

    // extend
    @Expose
    private String extension;

    @Expose
    private String url;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocalFileResource storage = (LocalFileResource) o;

        return id != null ? id.equals(storage.id) : storage.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "LocalFileResource{" +
            "name='" + name + '\'' +
            ", id='" + id + '\'' +
            ", extension='" + extension + '\'' +
            '}';
    }
}
