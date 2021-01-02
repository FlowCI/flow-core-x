package com.flowci.core.agent.domain;

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
 *
 * Selector id -> flow id > list of current job build number
 */
@Getter
@Setter
@Document(collection = "agent_priority")
public class AgentPriority extends Mongoable {

    @NonNull
    @Indexed(unique = true)
    private String selectorId;

    private Map<String, Set<Long>> queue = new HashMap<>();
}
