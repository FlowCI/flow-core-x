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

package com.flowci.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.common.helper.StringHelper;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"})
@NoArgsConstructor
@Accessors(chain = true)
public class Input implements Serializable {

    private String name;

    private String alias;

    private VarType type = VarType.STRING;

    private boolean required = true;

    // default value
    private String value;

    public Input(String name) {
        this.name = name;
    }

    public Input(String name, VarType type) {
        this.name = name;
        this.type = type;
    }

    @JsonIgnore
    public boolean hasDefaultValue() {
        return StringHelper.hasValue(value);
    }

    @JsonIgnore
    public int getIntDefaultValue() {
        return Integer.parseInt(this.value);
    }
}
