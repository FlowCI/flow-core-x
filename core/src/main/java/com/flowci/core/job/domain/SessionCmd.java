package com.flowci.core.job.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SessionCmd {

    private String jobId;

    private String script;
}
