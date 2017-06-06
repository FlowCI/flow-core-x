package com.flow.platform.domain;

/**
 * Created by gy@fir.im on 06/06/2017.
 * Copyright fir.im
 */
public class Zone {

    /**
     * Zone name, unique
     * Zone zk node path is /{root name}/{zone name}
     */
    private String name;

    /**
     * Cloud provider for manager instance
     */
    private String cloudProvider;

    public Zone() {
    }

    public Zone(String name, String cloudProvider) {
        this.name = name;
        this.cloudProvider = cloudProvider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Zone zone = (Zone) o;

        return name.equals(zone.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
