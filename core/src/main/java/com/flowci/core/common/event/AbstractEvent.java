package com.flowci.core.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public abstract class AbstractEvent<T> extends ApplicationEvent {

    private T fetched;

    private RuntimeException error;

    public AbstractEvent(Object source) {
        super(source);
    }

    public boolean hasError() {
        return error != null;
    }
}
