package com.flow.platform.cmd;

/**
 * Created by gy@fir.im on 15/06/2017.
 * Copyright fir.im
 */
public final class Log {

    public enum Type {
        STDOUT,
        STDERR,
    }

    public Log(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    private Type type;

    private String content;

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Log{" +
                "type=" + type +
                ", content='" + content + '\'' +
                '}';
    }
}
