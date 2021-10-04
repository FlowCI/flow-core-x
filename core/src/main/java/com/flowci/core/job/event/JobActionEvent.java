package com.flowci.core.job.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobActionEvent extends ApplicationEvent {

    public static final String ACTION_TO_RUN = "toRun";

    public static final String ACTION_TO_TIMEOUT = "toTimeout";

    private final String jobId;

    private final String action;

    public JobActionEvent(Object source, String jobId, String action) {
        super(source);
        this.jobId = jobId;
        this.action = action;
    }

    public boolean isToRun() {
        return this.action.equals(ACTION_TO_RUN);
    }

    public boolean isToTimeOut() {
        return this.action.equals(ACTION_TO_TIMEOUT);
    }
}
