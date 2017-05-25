package com.flow.platform.domain;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */

import java.io.Serializable;

/**
 * Config for server url
 */
public class AgentConfig implements Serializable {

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
}
