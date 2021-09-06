package com.flowci.core.agent.event;

import lombok.Getter;

public class OnCmdOutEvent extends EventFromAgent {

    @Getter
    private final byte[] raw;

    public OnCmdOutEvent(Object source, byte[] raw) {
        super(source, null, null);
        this.raw = raw;
    }
}
