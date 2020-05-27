package com.flowci.core.job.event;

import com.flowci.core.job.domain.ExecutedLocalTask;
import lombok.Getter;

import java.util.List;

@Getter
public class TaskUpdateEvent extends ExecutedEvent<ExecutedLocalTask> {

    public TaskUpdateEvent(Object source, String jobId, List<ExecutedLocalTask> items, boolean init) {
        super(source, jobId, items, init);
    }
}
