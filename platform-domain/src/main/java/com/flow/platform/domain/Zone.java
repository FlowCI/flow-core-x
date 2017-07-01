package com.flow.platform.domain;

/**
 * Created by gy@fir.im on 06/06/2017.
 * Copyright fir.im
 */
public class Zone extends Jsonable {

    /**
     * Zone name, unique
     * Zone zk node path is /{root name}/{zone name}
     */
    private String name;

    /**
     * Cloud provider for manager instance
     */
    private String cloudProvider;

    /**
     * Zone instance image name
     */
    private String imageName;

    /**
     * Minimum idle agent pool size, default is 1
     */
    private Integer minPoolSize = 1;

    /**
     * Maximum idle agent pool size, default is 1
     */
    private Integer maxPoolSize = 1;

    /**
     * Num of instance to start while idle agent not enough
     */
    private Integer numOfStart = minPoolSize;

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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getNumOfStart() {
        return numOfStart;
    }

    public void setNumOfStart(Integer numOfStart) {
        this.numOfStart = numOfStart;
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

    @Override
    public String toString() {
        return "Zone{" +
                "name='" + name + '\'' +
                ", cloudProvider='" + cloudProvider + '\'' +
                '}';
    }
}
