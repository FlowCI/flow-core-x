package com.flow.platform.cc.exception;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
public class AgentErr {

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String agentName) {
            super(String.format("AgentErr '%s' not found", agentName));
        }
    }

    public static class BusyException extends RuntimeException {
        public BusyException(String agentName) {
            super(String.format("Cannot receive command since agent '%s' is busy", agentName));
        }
    }
}
