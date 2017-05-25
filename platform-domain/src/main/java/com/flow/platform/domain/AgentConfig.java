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
     * Url for socket io realtime logging
     */
    private String loggingUrl;

    /**
     * Url for report agent status
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

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
