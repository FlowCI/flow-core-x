package com.flowci.core.job.event;

import com.flowci.core.common.event.BroadcastEvent;
import lombok.Getter;

@Getter
public class CacheShellLogEvent extends BroadcastEvent {

    private final String jobId;

    private final String stepId;

    private final byte[] log;

    public CacheShellLogEvent(Object source, String jobId, String stepId, byte[] log) {
        super(source);
        this.jobId = jobId;
        this.stepId = stepId;
        this.log = log;
    }
}
