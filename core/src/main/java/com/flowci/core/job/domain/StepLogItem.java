package com.flowci.core.job.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class StepLogItem {

    private String id;

    private String content;
}
