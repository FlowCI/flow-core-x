package com.flowci.core.job.event;

import com.flowci.core.common.event.BroadcastEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheShellLogEvent extends BroadcastEvent {

    private String jobId;

    private String stepId;

    private byte[] body; // StepLogItem json byte string

    public CacheShellLogEvent() {
       super();
    }

    public CacheShellLogEvent(Object source, String jobId, String stepId, byte[] body) {
        super(source);
        this.jobId = jobId;
        this.stepId = stepId;
        this.body = body;
    }
}
