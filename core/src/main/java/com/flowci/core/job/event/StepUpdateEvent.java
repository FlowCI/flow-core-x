package com.flowci.core.job.event;

import com.flowci.core.job.domain.Step;

import java.util.List;

public class StepUpdateEvent extends ExecutedEvent<Step> {

    public StepUpdateEvent(Object source, String jobId, List<Step> items, boolean init) {
        super(source, jobId, items, init);
    }
}
