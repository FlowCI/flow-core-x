package com.flow.platform.domain;

/**
 * Config for agent
 *
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public class AgentConfig extends Jsonable {

    /**
     * Url for socket io realtime logging
     */
    private String loggingUrl;

    /**
     * Url for report cmd status
     */
    private String cmdStatusUrl;

    public AgentConfig() {
    }

    public AgentConfig(String loggingUrl, String cmdStatusUrl) {
        this.loggingUrl = loggingUrl;
        this.cmdStatusUrl = cmdStatusUrl;
    }

    public String getLoggingUrl() {
        return loggingUrl;
    }

    public void setLoggingUrl(String loggingUrl) {
        this.loggingUrl = loggingUrl;
    }

    public String getCmdStatusUrl() {
        return cmdStatusUrl;
    }

    public void setCmdStatusUrl(String cmdStatusUrl) {
        this.cmdStatusUrl = cmdStatusUrl;
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                ", loggingUrl='" + loggingUrl + '\'' +
                ", cmdStatusUrl='" + cmdStatusUrl + '\'' +
                '}';
    }
}
