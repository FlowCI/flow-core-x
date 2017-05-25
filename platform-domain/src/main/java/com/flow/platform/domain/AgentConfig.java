package com.flow.platform.domain;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */

import com.google.gson.Gson;
import java.io.Serializable;

/**
 * Config for server url
 */
public class AgentConfig implements Serializable {

    /**
     * Get AgentConfig from json of byte[] format
     *
     * @param raw byte array from zk data
     * @return AgentConfig object
     */
    public static AgentConfig parse(byte[] raw) {
        String json = new String(raw);
        Gson gson = new Gson();
        return gson.fromJson(json, AgentConfig.class);
    }

    /**
     * Zookeeper url
     */
    private String zooKeeperUrl;

    /**
     * Url for socket io realtime logging
     */
    private String loggingUrl;

    /**
     * Url for report agent status
     */
    private String statusUrl;

    public AgentConfig() {
    }

    public AgentConfig(String zooKeeperUrl, String loggingUrl, String statusUrl) {
        this.loggingUrl = loggingUrl;
        this.statusUrl = statusUrl;
    }

    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public String getLoggingUrl() {
        return loggingUrl;
    }

    public void setLoggingUrl(String loggingUrl) {
        this.loggingUrl = loggingUrl;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "zooKeeperUrl='" + zooKeeperUrl + '\'' +
                ", loggingUrl='" + loggingUrl + '\'' +
                ", statusUrl='" + statusUrl + '\'' +
                '}';
    }
}
