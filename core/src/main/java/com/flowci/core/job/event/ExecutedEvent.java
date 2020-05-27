package com.flowci.core.job.event;

import com.flowci.core.job.domain.Executed;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public abstract class ExecutedEvent<T extends Executed> extends ApplicationEvent {

    private final String jobId;

    private final List<T> items;

    private final boolean init;

    protected ExecutedEvent(Object source, String jobId, List<T> items, boolean init) {
        super(source);
        this.jobId = jobId;
        this.items = items;
        this.init = init;
    }
}
