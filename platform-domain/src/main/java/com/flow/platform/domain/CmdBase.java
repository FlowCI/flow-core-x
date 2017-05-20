package com.flow.platform.domain;

import java.io.Serializable;

/**
 * Only include basic properties of command
 *
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
     * Target zone
     */
    private String zone;

    /**
     * Target agent
     */
    private String agent;

    /**
     * Command type
     */
    private CmdBase.Type type;

    /**
     * Command content
     */
    private String cmd;

    public CmdBase() {
    }

    public CmdBase(String zone, String agent, Type type, String cmd) {
        this.zone = zone;
        this.agent = agent;
        this.type = type;
        this.cmd = cmd;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Cmd.Type getType() {
        return type;
    }

    public void setType(Cmd.Type type) {
        this.type = type;
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

        if (!zone.equals(cmdBase.zone)) return false;
        if (!agent.equals(cmdBase.agent)) return false;
        if (type != cmdBase.type) return false;
        return cmd.equals(cmdBase.cmd);
    }

    @Override
    public int hashCode() {
        int result = zone.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + cmd.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CmdBase{" +
                "zone='" + zone + '\'' +
                ", agent='" + agent + '\'' +
                ", type=" + type +
                ", cmd='" + cmd + '\'' +
                '}';
    }
}
