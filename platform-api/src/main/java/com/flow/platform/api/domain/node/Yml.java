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

package com.flow.platform.api.domain.node;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Yml raw content
 *
 * @author yh@firim
 */

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"nodePath"})
@ToString
public class Yml {

    @Getter
    @Setter
    private String nodePath;

    @Getter
    @Setter
    private String file;

    public Yml(String nodePath) {
        this.nodePath = nodePath;
    }
}
