package com.flowci.core.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public abstract class AbstractSyncEvent<T> extends ApplicationEvent implements SyncEvent {

    private T fetched;

    private RuntimeException error;

    public AbstractSyncEvent(Object source) {
        super(source);
    }

    public boolean hasError() {
        return error != null;
    }
}
