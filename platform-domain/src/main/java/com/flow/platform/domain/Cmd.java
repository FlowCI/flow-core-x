package com.flow.platform.domain;

import com.google.gson.Gson;

/**
 * Command object to communicate between c/s
 *
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Cmd extends CmdBase {

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

    public enum Status {

        /**
         * Init status when cmd send to agent
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
    private Cmd.Status status = Status.PENDING;


    public Cmd() {
    }

    public Cmd(CmdBase cmdBase) {
        super(cmdBase.getZone(),
                cmdBase.getAgent(),
                cmdBase.getSocketIoServer(),
                cmdBase.getType(),
                cmdBase.getCmd());
    }

    public Cmd(String zone, String agent, String socketIoSever, Type type, String cmd) {
        super(zone, agent, socketIoSever, type, cmd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Cmd cmd = (Cmd) o;

        return id != null ? id.equals(cmd.id) : cmd.id == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
