package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.Notification;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

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

    /**
     * Notification list that run locally
     */
    private List<Notification> notifications = new LinkedList<>();

    /**
     * The final step that will be executed anyway
     */
    private List<StepNode> after = new LinkedList<>();

    public FlowNode(String name) {
        super(name);
    }

    @JsonIgnore
    public boolean hasCron() {
        return !Strings.isNullOrEmpty(cron);
    }
}
