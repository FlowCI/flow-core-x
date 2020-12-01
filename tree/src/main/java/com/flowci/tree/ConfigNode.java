package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * The node could have children
 */
@Getter
@Setter
public abstract class ConfigNode extends Node {

    /**
     * Node before groovy script;
     */
    protected String condition;

    /**
     * Inner option has higher priority
     * Ex: Plugin > Step > Flow
     */
    protected List<DockerOption> dockers = new LinkedList<>();

    /**
     * Input environment variables
     */
    protected StringVars environments = new StringVars();

    public ConfigNode(String name, Node parent) {
        super(name, parent);
    }

    public String getEnv(String name) {
        return environments.get(name);
    }

    @JsonIgnore
    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }

    @JsonIgnore
    public boolean hasDocker() {
        return !dockers.isEmpty();
    }

    @JsonIgnore
    public boolean isConfigurableParent() {
        return super.hasParent() && parent instanceof ConfigNode;
    }
}
