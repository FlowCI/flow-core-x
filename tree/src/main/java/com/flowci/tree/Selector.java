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

package com.flowci.tree;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Agent selector
 *
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "label")
public class Selector implements Serializable {

    public static final Selector EMPTY = new Selector();

    private Set<String> label = Collections.emptySet();

    public Selector(String... labels) {
        this.label = Set.of(labels);
    }

    public String getId() {
        return "" + label.hashCode();
    }
}
