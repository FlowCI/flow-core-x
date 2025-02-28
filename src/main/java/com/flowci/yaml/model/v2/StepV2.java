package com.flowci.yaml.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowci.common.model.Variables;
import com.flowci.yaml.model.Step;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = "name", callSuper = false)
public class StepV2 extends BaseV2 implements Step<DockerV2, StepV2, CommandV2> {

    private static final Boolean DEFAULT_ALLOW_FAILURE = false;

    private String name;

    private Set<String> agents;

    // dependency steps name
    @JsonProperty("depends_on")
    private List<String> dependsOn;

    private String plugin;

    private Integer retry; // num of retry

    @JsonProperty("allow_failure")
    private Boolean allowFailure = DEFAULT_ALLOW_FAILURE;

    private List<CommandV2> commands;

    /**
     * Output environment variables
     */
    private List<String> output;

    private List<String> secrets;

    private List<String> configs;

    // ref to parent flow
    @JsonIgnore
    private FlowV2 parent;

    // ref to next steps
    private List<StepV2> next = new LinkedList<>();

    @Override
    public DockerV2 getDocker() {
        if (this.docker != null) {
            return this.docker;
        }

        if (this.parent.docker != null) {
            return this.parent.docker;
        }

        return null;
    }

    @Override
    public List<DockerV2> getDockers() {
        if (this.dockers != null) {
            return this.dockers;
        }

        if (this.parent.dockers != null) {
            return this.parent.dockers;
        }

        return null;
    }

    // merge variables from current step and parent flow
    @Override
    public Variables getVariables() {
        var variables = new Variables(parent.getVariables());
        variables.putAll(this.variables);
        return variables;
    }
}
