package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class StepNode extends Node {

    public final static boolean ALLOW_FAILURE_DEFAULT = false;

    public final static boolean IS_TAIL_DEFAULT = false;

    /**
     * Node before groovy script;
     */
    private String before;

    /**
     * Node execute script, can be null
     */
    private String script;

    /**
     * Plugin name
     */
    private String plugin;

    private Set<String> exports = new HashSet<>(0);

    private int order;

    /**
     * Is allow failure
     */
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    private boolean tail = IS_TAIL_DEFAULT;

    public StepNode(String name) {
        super(name);
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return !Strings.isNullOrEmpty(plugin);
    }

    @JsonIgnore
    public boolean hasBefore() {
        return !Strings.isNullOrEmpty(before);
    }

    @JsonIgnore
    public boolean hasExports() {
        return exports != null && !exports.isEmpty();
    }

}
