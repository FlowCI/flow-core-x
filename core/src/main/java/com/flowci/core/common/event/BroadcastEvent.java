package com.flowci.core.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public abstract class BroadcastEvent extends ApplicationEvent {

    private final static Object Source = new Object();

    private boolean internal;

    public BroadcastEvent() {
        super(Source);
    }

    public BroadcastEvent(Object source) {
        super(source);
    }
}
