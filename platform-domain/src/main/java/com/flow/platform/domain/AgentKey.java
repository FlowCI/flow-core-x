package com.flow.platform.domain;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public class AgentKey implements Serializable {

    public final static String RESERVED_CHAR = "#";

    private String zone;

    private String name;

    public AgentKey(String zone, String name) {
        if (zone.contains(RESERVED_CHAR) || name.contains(RESERVED_CHAR)) {
            throw new IllegalArgumentException("Agent key not valid");
        }
        this.zone = zone;
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

        AgentKey agentKey = (AgentKey) o;

        if (!zone.equals(agentKey.zone)) return false;
        return name.equals(agentKey.name);
    }

    @Override
    public int hashCode() {
        int result = zone.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s#%s", zone, name);
    }
}
