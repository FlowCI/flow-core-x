package com.flowci.domain.node;

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
    protected List<DockerOption> dockers;

    /**
     * Parent node in origin config
     */
    protected Node parent;

    /**
     * Children steps in origin config
     */
    protected List<StepNode> steps;

    public boolean hasParent() {
        return parent != null;
    }
}
