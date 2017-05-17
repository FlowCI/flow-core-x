package com.flow.platform.util.zk;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
public class ZkNodeBuilder {

    private String root;
    private String zone;
    private String agent;

    private StringBuilder path = new StringBuilder();

    private ZkNodeBuilder(String root) {
        this.root = root;
        this.path.append("/").append(root);
    }

    public ZkNodeBuilder zone(String zone) {
        this.zone = zone;
        this.path.append("/").append(zone);
        return this;
    }

    public ZkNodeBuilder agent(String agent) {
        this.agent = agent;
        this.path.append("/").append(agent);
        return this;
    }

    public String path() {
        return path.toString();
    }

    public String busy() {
        if (agent == null) {
            return null;
        }
        return String.format("%s-busy", path.toString());
    }

    public String getRoot() {
        return root;
    }

    public String getZone() {
        return zone;
    }

    public String getAgent() {
        return agent;
    }

    public static ZkNodeBuilder create(String root) {
        return new ZkNodeBuilder(root);
    }
}
