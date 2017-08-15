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

/**
 * @author yh@firim
 */
public class YmlStorage {

    private String nodePath;

    private String file;

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public YmlStorage(String nodePath, String file) {
        this.nodePath = nodePath;
        this.file = file;
    }

    public YmlStorage() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        YmlStorage that = (YmlStorage) o;

        return nodePath != null ? nodePath.equals(that.nodePath) : that.nodePath == null;
    }

    @Override
    public int hashCode() {
        int result = nodePath != null ? nodePath.hashCode() : 0;
        result = 31 * result + (file != null ? file.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "YmlStorage{" +
            "nodePath='" + nodePath + '\'' +
            ", file='" + file + '\'' +
            '}';
    }
}
