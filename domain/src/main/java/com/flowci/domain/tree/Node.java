package com.flowci.domain.tree;

import com.flowci.domain.StringVars;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode(of = "path")
@SuperBuilder
@NoArgsConstructor
public abstract class Node implements Serializable {

    /**
     * Node name
     */
    protected String name;

    /**
     * Node path
     */
    protected NodePath path;

    /**
     * Environment variables
     */
    protected StringVars vars;

    /**
     * Node before groovy script;
     */
    protected String condition;

    /**
     * Agent tags
     */
    protected List<String> agents;

    /**
     * Docker options
     */
    protected DockerOption docker;

    /**
     * Parent node of tree structure
     */
    protected Node parent;

    /**
     * Children steps represent origin yaml config
     */
    protected List<StepNode> steps;

    /**
     * Children steps represent tree structure
     */
    protected List<StepNode> children;

    public boolean hasParent() {
        return parent != null;
    }
}
