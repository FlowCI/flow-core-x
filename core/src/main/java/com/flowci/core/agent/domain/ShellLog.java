package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ShellLog {

    private String jobId;

    private String stepId;

    private String log;
}
