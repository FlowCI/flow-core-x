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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yang
 */
@RequiredArgsConstructor
@EqualsAndHashCode
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
    @Getter
    private final String name;

    @Expose
    @Getter
    private final String tag;

    @Override
    public String toString() {
        return name + "[" + tag + "]";
    }
}
