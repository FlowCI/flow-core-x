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
package com.flow.platform.api.domain.request;

import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * @author lhl
 */
public class ListParam <T> {

    @Expose
    private List<T> arrays;

    public List<T> getArrays() {
        return arrays;
    }

    public void setArrays(List<T> arrays) {
        this.arrays = arrays;
    }
}
