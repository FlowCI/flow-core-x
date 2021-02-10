package com.flowci.core.job.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Record all RUNNING jobs (flow, build number) for selector
 * To prioritize which job has higher priority for agent dispatching
 */
@Getter
@Setter
@Document(collection = "job_priority")
public class JobPriority extends Mongoable {

    @NonNull
    @Indexed(unique = true)
    private String flowId;

    // ongoing job build number that received from queue into application
    private List<Long> queue = new ArrayList<>();
}
