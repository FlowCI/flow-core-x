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

package com.flowci.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
public class StringVars extends Vars<String> {

    public static final StringVars EMPTY = new StringVars(0);

    public StringVars() {
        super();
    }

    public StringVars(int size) {
        super(size);
    }

    public StringVars(Map<String, String> data) {
        super(data.size());
        merge(data);
    }

    public StringVars(Vars<String> data) {
        super(data.size());
        merge(data);
    }

    public void mergeFromTypedVars(Vars<VarValue> typedVars) {
        for (Map.Entry<String, VarValue> entry : typedVars.entrySet()) {
            put(entry.getKey(), entry.getValue().getData());
        }
    }

    @Override
    public List<String> toList() {
        List<String> list = new ArrayList<>(this.size());
        for (Map.Entry<String, String> entry : this.entrySet()) {
            list.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        return list;
    }
}
