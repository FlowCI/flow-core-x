package com.flowci.core.task.event;

import com.flowci.core.common.event.SyncEvent;
import com.flowci.core.task.domain.LocalTask;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Mark to SyncEvent, it will be handled by task executor
 */
@Getter
public class StartAsyncLocalTaskEvent extends ApplicationEvent implements SyncEvent {

    private final LocalTask task;

    public StartAsyncLocalTaskEvent(Object source, LocalTask task) {
        super(source);
        this.task = task;
    }
}
