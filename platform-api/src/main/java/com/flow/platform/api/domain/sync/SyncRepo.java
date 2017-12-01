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

package com.flow.platform.api.domain.sync;

import com.google.gson.annotations.Expose;

/**
 * @author yang
 */
public class SyncRepo {

    /**
     * Create SyncRepo instance from string ex: A[v1.0]
     *
     * @return SyncRepo instance or {@code null} if str is illegal
     */
    public static SyncRepo build(String str) {
        if (!str.endsWith("]")) {
            return null;
        }

        int indexOfLeftBracket = str.lastIndexOf('[');
        if (indexOfLeftBracket == -1) {
            return null;
        }

        String name = str.substring(0, indexOfLeftBracket);
        String tag = str.substring(indexOfLeftBracket + 1, str.length() - 1);
        return new SyncRepo(name, tag);
    }

    @Expose
    private String name;

    @Expose
    private String tag;

    public SyncRepo(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SyncRepo syncRepo = (SyncRepo) o;

        if (!name.equals(syncRepo.name)) {
            return false;
        }
        return tag.equals(syncRepo.tag);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + tag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return name + "[" + tag + "]";
    }
}
