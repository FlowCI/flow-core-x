package com.flowci.core.job.event;

import com.flowci.core.common.event.AbstractEvent;
import com.flowci.domain.LocalTask;
import lombok.Getter;

/**
 * Mark to SyncEvent, it will be handled by task executor
 */
@Getter
public class StartAsyncLocalTaskEvent extends AbstractEvent<Void> {

    private final LocalTask task;

    public StartAsyncLocalTaskEvent(Object source, LocalTask task) {
        super(source);
        this.task = task;
    }
}
