package com.flowci.core.agent.domain;

import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.tree.Cache;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
public final class ShellIn extends CmdIn {

    public enum ShellType {
        Bash,

        PowerShell;
    }

    // from ExecutedCmd id
    private String id;

    private String flowId;

    private String jobId;

    private boolean allowFailure;

    private String plugin;

    private Cache cache;

    private List<DockerOption> dockers;

    private List<String> bash;

    private List<String> pwsh;

    private int timeout = 1800; // from StepNode.timeout

    private int retry; // from StepNode.retry

    private Vars<String> inputs;

    private Set<String> envFilters;

    public ShellIn() {
        super(Type.SHELL);
    }

    public void addScript(String script, ShellType type) {
        if (Strings.isNullOrEmpty(script)) {
            return;
        }

        if (type == ShellType.Bash) {
            bash.add(script);
        }

        if (type == ShellType.PowerShell) {
            pwsh.add(script);
        }
    }

    public void addEnvFilters(Set<String> exports) {
        this.envFilters.addAll(exports);
    }

    public void addInputs(StringVars vars) {
        inputs.putAll(vars);
    }
}
