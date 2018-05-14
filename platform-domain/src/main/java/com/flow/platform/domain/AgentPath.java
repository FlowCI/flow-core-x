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

package com.flow.platform.domain;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent path in zookeeper
 *
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"zone", "name"}, callSuper = false)
public class AgentPath extends Jsonable {

    private final static String RESERVED_CHAR = "#";

    @Setter
    @Getter
    @Expose
    private String zone;

    @Setter
    @Getter
    @Expose
    private String name;

    public AgentPath(String zone, String name) {
        if (zone != null && zone.contains(RESERVED_CHAR)) {
            throw new IllegalArgumentException("Agent key not valid");
        }

        // name is nullable
        if (name != null && name.contains(RESERVED_CHAR)) {
            throw new IllegalArgumentException("Agent key not valid");
        }

        this.zone = zone;
        this.name = name;
    }

    /**
     * Is empty zone and name
     */
    public boolean isEmpty() {
        return !hasZone() || !hasName();
    }

    public boolean hasZone() {
        return !Strings.isNullOrEmpty(zone);
    }

    public boolean hasName() {
        return !Strings.isNullOrEmpty(name);
    }

    @Override
    public String toString() {
        return zone + RESERVED_CHAR + name;
    }
}
