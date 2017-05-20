package com.flow.platform.domain;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Command object to communicate between c/s
 *
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Cmd implements Serializable {

    /**
     * Get zk cmd from json of byte[] format
     *
     * @param raw
     * @return Cmd or null if any exception
     */
    public static Cmd parse(byte[] raw) {
        String json = new String(raw);
        Gson gson = new Gson();
        return gson.fromJson(json, Cmd.class);
    }

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

    public enum Status {

        /**
         *
         */
        PENDING("PENDING"),

        /**
         * Cmd running
         */
        RUNNING("RUNNING"),

        /**
         * Cmd executed
         */
        EXECUTED("EXECUTED"),

        /**
         * Log uploaded
         */
        LOGGED("LOGGED"),

        /**
         * Got exception when running
         */
        EXCEPTION("EXCEPTION"),

        /**
         * Cmd done
         */
        DONE("DONE");

        private String name;

        Status(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Server generated command id
     */
    private String id;

    /**
     * Cmd status
     */
    private Cmd.Status status;

    /**
     * Command type
     */
    private Cmd.Type type;

    /**
     * Command content
     */
    private String cmd;

    public Cmd() {
    }

    public Cmd(Type type, String cmd) {
        this.type = type;
        this.cmd = cmd;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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

        Cmd cmd = (Cmd) o;

        if (type != cmd.type) return false;
        return this.cmd != null ? this.cmd.equals(cmd.cmd) : cmd.cmd == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (cmd != null ? cmd.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cmd{" +
                "type=" + type +
                ", cmd='" + cmd + '\'' +
                '}';
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
