/*
 * Copyright 2019 flow.ci
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

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VarValue implements Serializable {

    public static VarValue of(String value, VarType type) {
        VarValue v = new VarValue();
        v.setData(value);
        v.setType(type);
        return v;
    }

    public static VarValue of(String value, VarType type, boolean editable) {
        VarValue v = VarValue.of(value, type);
        v.setEditable(editable);
        return v;
    }

    private String data;

    private VarType type;

    private boolean editable = true;
}
