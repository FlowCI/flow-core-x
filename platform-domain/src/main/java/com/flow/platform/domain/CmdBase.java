package com.flow.platform.domain;

import java.io.Serializable;

/**
 * Only include basic properties of command
 * <p>
 * Created by gy@fir.im on 20/05/2017.
 * Copyright fir.im
 */
public class CmdBase implements Serializable {

    public enum Type {
        /**
         * Run a shell script in agent
         */
        RUN_SHELL("RUN_SHELL"),

        /**
         * KILL current running processes
         */
        KILL("KILL"),

        /**
         * Stop agent
         */
        STOP("STOP"),

        /**
         * Stop agent and shutdown machine
         */
        SHUTDOWN("SHUTDOWN");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Config for server url
     */
    public static class Config implements Serializable {

        /**
         * Url for socket io realtime logging
         */
        private String loggingUrl;

        /**
         * Url for report agent status
         */
        private String statusUrl;

        public Config() {
        }

        public Config(String loggingUrl, String statusUrl) {
            this.loggingUrl = loggingUrl;
            this.statusUrl = statusUrl;
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

    /**
     * Destination of command
     */
    private AgentPath agentPath;

    /**
     * Command type (Required)
     */
    private CmdBase.Type type;

    /**
     * Server communication configuration (Nullable)
     */
    private CmdBase.Config config;

    /**
     * Command content (Required when type = RUN_SHELL)
     */
    private String cmd;

    public CmdBase() {
    }

    public CmdBase(String zone, String agent, CmdBase.Config config, Type type, String cmd) {
        this(new AgentPath(zone, agent), config, type, cmd);
    }

    public CmdBase(AgentPath agentPath, CmdBase.Config config, Type type, String cmd) {
        this.agentPath = agentPath;
        this.config = config;
        this.type = type;
        this.cmd = cmd;
    }

    public AgentPath getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(AgentPath agentPath) {
        this.agentPath = agentPath;
    }

    public String getZone() {
        return agentPath.getZone();
    }

    public String getAgent() {
        return agentPath.getName();
    }

    public Cmd.Type getType() {
        return type;
    }

    public void setType(Cmd.Type type) {
        this.type = type;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CmdBase cmdBase = (CmdBase) o;

        if (!agentPath.equals(cmdBase.agentPath)) return false;
        if (type != cmdBase.type) return false;
        return cmd.equals(cmdBase.cmd);
    }

    @Override
    public int hashCode() {
        int result = agentPath.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + cmd.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CmdBase{" +
                "zone='" + agentPath.getZone() + '\'' +
                ", agent='" + agentPath.getName() + '\'' +
                ", type=" + type +
                ", cmd='" + cmd + '\'' +
                '}';
    }
}
