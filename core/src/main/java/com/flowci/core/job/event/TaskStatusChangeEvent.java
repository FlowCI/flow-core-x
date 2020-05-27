package com.flowci.core.job.event;

import com.flowci.core.job.domain.ExecutedLocalTask;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class TaskStatusChangeEvent extends ApplicationEvent {

    private final String jobId;

    private final List<ExecutedLocalTask> tasks;

    public TaskStatusChangeEvent(Object source, String jobId, List<ExecutedLocalTask> tasks) {
        super(source);
        this.jobId = jobId;
        this.tasks = tasks;
    }
}
