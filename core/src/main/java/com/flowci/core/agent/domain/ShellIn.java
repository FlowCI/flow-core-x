package com.flowci.core.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
public final class ShellIn extends CmdIn {

    // from ExecutedCmd id
    private String id;

    private String flowId;

    private String jobId;

    private boolean allowFailure;

    private String plugin;

    private List<DockerOption> dockers;

    @JsonIgnore
    private String condition;

    private List<String> scripts = new LinkedList<>();

    private int timeout = 1800;

    private Vars<String> inputs = new StringVars();

    private Set<String> envFilters = new LinkedHashSet<>();

    public ShellIn() {
        super(Type.SHELL);
    }

    public void addScript(String script) {
        if (Strings.isNullOrEmpty(script)) {
            return;
        }
        scripts.add(script);
    }

    public void addEnvFilters(Set<String> exports) {
        this.envFilters.addAll(exports);
    }

    public void addInputs(StringVars vars) {
        inputs.putAll(vars);
    }

    @JsonIgnore
    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }
}
