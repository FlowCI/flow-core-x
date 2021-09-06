package com.flowci.core.agent.event;

import lombok.Getter;

@Getter
public class OnTTYLogEvent extends EventFromAgent {

    private final String ttyId;

    private final String body;

    public OnTTYLogEvent(Object source, String ttyId, String body) {
        super(source, null, null);
        this.ttyId = ttyId;
        this.body = body;
    }
}
