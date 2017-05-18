package com.flow.platform.util.zk;

/**
 * To create path by root/append/agent
 *
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
public class ZkPathBuilder {

    private StringBuilder path = new StringBuilder();

    private ZkPathBuilder(String root) {
        this.path.append("/").append(root);
    }

    public ZkPathBuilder append(String node) {
        this.path.append("/").append(node);
        return this;
    }

    public String path() {
        return path.toString();
    }

    public String busy() {
        return String.format("%s-busy", path.toString());
    }

    public static ZkPathBuilder create(String root) {
        return new ZkPathBuilder(root);
    }

    public static String busyNodeName(String nodeName) {
        return String.format("%s-busy", nodeName);
    }
}
