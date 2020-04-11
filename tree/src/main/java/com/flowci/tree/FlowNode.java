package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FlowNode extends Node {

    /**
     * Agent tags to set node running on which agent
     */
    private Selector selector;

    /**
     * Node start trigger
     */
    private TriggerFilter trigger = new TriggerFilter();

    /**
     * Unix cron expression
     */
    private String cron;

    public FlowNode(String name) {
        super(name);
    }

    @JsonIgnore
    public boolean hasCron() {
        return !Strings.isNullOrEmpty(cron);
    }
}
