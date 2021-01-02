package com.flowci.core.job.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * To list all jobs (flow, build number) for selector
 */
@Getter
@Setter
@Document(collection = "job_priority")
public class JobPriority extends Mongoable {

    @NonNull
    @Indexed(unique = true)
    private String flowId;

    // key as selector id, value as list of current job build number
    private Map<String, Set<Long>> queue = new HashMap<>();
}
