package com.flow.platform.util.zk;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 12/05/2017.
 *
 * @copyright fir.im
 */
public class ZkCmd implements Serializable {

    /**
     * Get zk cmd from json of byte[] format
     *
     * @param raw
     * @return ZkCmd or null if any exception
     */
    public static ZkCmd parse(byte[] raw) {
        try {
            String json = new String(raw);
            Gson gson = new Gson();
            return gson.fromJson(json, ZkCmd.class);
        } catch (Throwable e) {
            return null;
        }
    }

    public enum Type {
        /**
         * Run a shell script in agent
         */
        RUN_SHELL("RUN_SHELL"),

        /**
         * Stop current running processes
         */
        STOP("STOP"),

        /**
         * Close agent
         */
        CLOSE("CLOSE"),

        /**
         * Stop agent and close
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

    private ZkCmd.Type type;

    private String cmd;

    public ZkCmd() {
    }

    public ZkCmd(Type type, String cmd) {
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

        ZkCmd zkCmd = (ZkCmd) o;

        if (type != zkCmd.type) return false;
        return cmd != null ? cmd.equals(zkCmd.cmd) : zkCmd.cmd == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (cmd != null ? cmd.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ZkCmd{" +
                "type=" + type +
                ", cmd='" + cmd + '\'' +
                '}';
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
