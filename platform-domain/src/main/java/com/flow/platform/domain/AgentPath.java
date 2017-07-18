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

import java.io.Serializable;

/**
 * Agent path in zookeeper
 *
 * @author gy@fir.im
 */
public class AgentPath implements Serializable {

    private final static String RESERVED_CHAR = "#";

    private String zone;

    private String name;

    public AgentPath(String zone, String name) {
        if (zone.contains(RESERVED_CHAR)) {
            throw new IllegalArgumentException("Agent key not valid");
        }

        // name is nullable
        if (name != null && name.contains(RESERVED_CHAR)) {
            throw new IllegalArgumentException("Agent key not valid");
        }

        this.zone = zone;
        this.name = name;
    }

    public AgentPath() {
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZone() {
        return zone;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgentPath agentPath = (AgentPath) o;

        if (!zone.equals(agentPath.zone)) return false;
        return name != null ? name.equals(agentPath.name) : agentPath.name == null;
    }

    @Override
    public int hashCode() {
        int result = zone.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s#%s", zone, name);
    }
}
