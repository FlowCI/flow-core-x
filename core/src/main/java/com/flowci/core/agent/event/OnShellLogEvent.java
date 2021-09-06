package com.flowci.core.agent.event;

import lombok.Getter;

@Getter
public class OnShellLogEvent extends EventFromAgent {

    private final String jobId;

    private final String stepId;

    private final String b64Log;

    public OnShellLogEvent(Object source, String jobId, String stepId, String b64Log) {
        super(source, null, null);
        this.jobId = jobId;
        this.stepId = stepId;
        this.b64Log = b64Log;
    }
}
