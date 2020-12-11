package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.ObjectWrapper;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class RegularStepNode extends Node {

    public static final boolean ALLOW_FAILURE_DEFAULT = false;

    /**
     * bash script
     */
    private String bash;

    /**
     * powershell script
     */
    private String pwsh;

    /**
     * Plugin name
     */
    private String plugin;

    /**
     * Step timeout in seconds
     */
    private Integer timeout;

    /**
     * Num of retry times
     */
    private Integer retry; // num of retry

    /**
     * Env vars export to job context
     */
    private Set<String> exports = new HashSet<>(0);

    /**
     * Is allow failure
     */
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    /**
     * Cache setting
     */
    private Cache cache;

    /**
     * Sub steps
     */
    private List<Node> children = new LinkedList<>();

    public RegularStepNode(String name, Node parent) {
        super(name, parent);
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return !Strings.isNullOrEmpty(plugin);
    }

    @JsonIgnore
    public boolean hasCondition() {
        return !Strings.isNullOrEmpty(condition);
    }

    @JsonIgnore
    public boolean hasTimeout() {
        return timeout != null;
    }

    @JsonIgnore
    public boolean hasRetry() {
        return retry != null;
    }

    @JsonIgnore
    public List<String> fetchBash() {
        List<String> output = new LinkedList<>();
        this.forEachBottomUpStep(this, n -> {
            output.add(n.getBash());
            return true;
        });
        return output;
    }

    @JsonIgnore
    public List<String> fetchPwsh() {
        List<String> output = new LinkedList<>();
        this.forEachBottomUpStep(this, n -> {
            output.add(n.getPwsh());
            return true;
        });
        return output;
    }

    @JsonIgnore
    public Set<String> fetchFilters() {
        Set<String> output = new LinkedHashSet<>();
        this.forEachBottomUpStep(this, (n) -> {
            output.addAll((n).getExports());
            return true;
        });
        return output;
    }

    @JsonIgnore
    public Integer fetchTimeout(Integer defaultVal) {
        ObjectWrapper<Integer> wrapper = new ObjectWrapper<>(defaultVal);
        this.forEachBottomUpStep(this, (n) -> {
            if (n.hasTimeout()) {
                wrapper.setValue(n.getTimeout());
                return false;
            }
            return true;
        });
        return wrapper.getValue();
    }

    @JsonIgnore
    public Integer fetchRetry(Integer defaultVal) {
        ObjectWrapper<Integer> wrapper = new ObjectWrapper<>(defaultVal);
        this.forEachBottomUpStep(this, (n) -> {
            if (n.hasRetry()) {
                wrapper.setValue(n.getRetry());
                return false;
            }
            return true;
        });
        return wrapper.getValue();
    }

    protected void forEachBottomUpStep(Node node, Function<RegularStepNode, Boolean> onNode) {
        super.forEachBottomUp(node, (n) -> {
            if (n instanceof RegularStepNode) {
                return onNode.apply((RegularStepNode) n);
            }
            return true;
        });
    }
}
