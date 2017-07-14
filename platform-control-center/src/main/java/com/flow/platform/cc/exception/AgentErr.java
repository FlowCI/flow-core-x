package com.flow.platform.cc.exception;

import com.flow.platform.exception.FlowException;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
public class AgentErr {

    public static class NotFoundException extends FlowException {

        public NotFoundException(String agentName) {
            super(String.format("AgentErr '%s' not found", agentName), null);
        }
    }

    public static class NotAvailableException extends FlowException {

        public NotAvailableException(String agentName) {
            super(String
                    .format("Cannot receive command since agent '%s' is offline or busy", agentName),
                null);
        }
    }

    public static class AgentMustBeSpecified extends FlowException {

        public AgentMustBeSpecified() {
            super("Agent name must be specified when cmd type is not RUN_SHELL", null);
        }
    }
}
