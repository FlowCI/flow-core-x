package com.flowci.core.job.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public final class JobKey {

    private static final char Splitter = '-';

    private String flowId;

    private Long buildNumber;

    @Override
    public String toString() {
        return flowId + Splitter + buildNumber;
    }
}
