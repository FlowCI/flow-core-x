package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.common.domain.ObjectWrapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class FlowNode extends Node {

    public final static String DEFAULT_ROOT_NAME = "flow";

    /**
     * Agent tags to set node running on which agent
     */
    private Selector selector;

    private List<Node> children = new LinkedList<>();

    public FlowNode(String name, Node parent) {
        super(name, parent);
    }

    public FlowNode(String name) {
        super(name, null);
    }

    @JsonIgnore
    public Selector fetchSelector() {
        ObjectWrapper<Selector> wrapper = new ObjectWrapper<>(Selector.EMPTY);
        forEachBottomUp(this, n -> {
            if (n instanceof FlowNode) {
                FlowNode f = (FlowNode) n;
                if (f.hasSelector()) {
                    wrapper.setValue(f.selector);
                    return false;
                }
            }
            return true;
        });
        return wrapper.getValue();
    }

    @JsonIgnore
    public boolean hasSelector() {
        return !this.selector.getLabel().isEmpty();
    }
}
