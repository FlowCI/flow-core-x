package com.flowci.core.job.event;

import com.flowci.core.job.domain.ExecutedCmd;

import java.util.List;

public class StepUpdateEvent extends ExecutedEvent<ExecutedCmd> {

    public StepUpdateEvent(Object source, String jobId, List<ExecutedCmd> items, boolean init) {
        super(source, jobId, items, init);
    }
}
