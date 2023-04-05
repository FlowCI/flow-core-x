package com.flowci.domain.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.ObjectWrapper;
import com.flowci.util.StringHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StepNode extends Node {

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
     * Indicate the step is post step
     */
    private boolean post;

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
     * Included secret name in the step
     */
    private Set<String> secrets = new HashSet<>(0);

    /**
     * Included config name in the step
     */
    private Set<String> configs = new HashSet<>(0);

    /**
     * Is allow failure
     */
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    /**
     * Cache of step
     */
    private Set<CacheOption> caches;

    /**
     * Artifact of step
     */
    private Set<ArtifactOption> artifacts;

    /**
     * Indicate step as stage if there is no children steps
     */
    @JsonIgnore
    public boolean isStage() {
        return steps == null || steps.isEmpty();
    }

    @JsonIgnore
    public boolean hasSingleChild() {
        return !isStage() && steps.size() == 1;
    }

    @JsonIgnore
    public boolean hasMultiChildren() {
        return !isStage() && steps.size() > 1;
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return StringHelper.hasValue(plugin);
    }

    @JsonIgnore
    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }

    @JsonIgnore
    public boolean hasBash() {
        return StringHelper.hasValue(bash);
    }

    @JsonIgnore
    public boolean hasPwsh() {
        return StringHelper.hasValue(pwsh);
    }

    @JsonIgnore
    public boolean hasSecrets() {
        return secrets != null && !secrets.isEmpty();
    }

    @JsonIgnore
    public boolean hasExports() {
        return exports != null && !exports.isEmpty();
    }

    @JsonIgnore
    public boolean hasConfigs() {
        return configs != null && !configs.isEmpty();
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
    public boolean hasCaches() {
        return caches != null && !caches.isEmpty();
    }

    @JsonIgnore
    public boolean hasArtifact() {
        return artifacts != null && !artifacts.isEmpty();
    }

    @JsonIgnore
    public List<String> fetchBash() {
        var output = new LinkedList<String>();
        forEachBottomUp(this, n -> {
            if (n.hasBash()) {
                output.addFirst(n.bash);
            }
        });
        return output;
    }

    @JsonIgnore
    public List<String> fetchPwsh() {
        var output = new LinkedList<String>();
        forEachBottomUp(this, n -> {
            if (n.hasPwsh()) {
                output.addFirst(n.pwsh);
            }
        });
        return output;
    }

    @JsonIgnore
    public Integer fetchTimeout(Integer defaultVal) {
        var wrapper = new ObjectWrapper<>(defaultVal);
        forEachBottomUp(this, n -> {
            if (n.hasTimeout()) {
                wrapper.setValue(n.timeout);
                return false;
            }
            return true;
        });
        return wrapper.getValue();
    }

    @JsonIgnore
    public Integer fetchRetry(Integer defaultVal) {
        var wrapper = new ObjectWrapper<>(defaultVal);
        forEachBottomUp(this, n -> {
            if (n.hasRetry()) {
                wrapper.setValue(n.retry);
                return false;
            }
            return true;
        });
        return wrapper.getValue();
    }

    @JsonIgnore
    public Set<String> fetchExports() {
        var output = new LinkedHashSet<String>();
        forEachBottomUp(this, (n) -> {
            if (n.hasExports()) {
                output.addAll(n.exports);
            }
        });
        return output;
    }

    @JsonIgnore
    public Set<String> fetchSecrets() {
        var output = new LinkedHashSet<String>();
        forEachBottomUp(this, (n) -> {
            if (n.hasSecrets()) {
                output.addAll(n.secrets);
            }
        });
        return output;
    }

    @JsonIgnore
    public Set<String> fetchConfig() {
        var output = new LinkedHashSet<String>();
        forEachBottomUp(this, (n) -> {
            if (n.hasConfigs()) {
                output.addAll(n.configs);
            }
        });
        return output;
    }

    @JsonIgnore
    public Set<CacheOption> fetchCacheOption() {
        var output = new LinkedHashSet<CacheOption>();
        forEachBottomUp(this, (n) -> {
            if (n.hasCaches()) {
                output.addAll(n.caches);
            }
        });
        return output;
    }

    @JsonIgnore
    public Set<ArtifactOption> fetchArtifactOption() {
        var output = new LinkedHashSet<ArtifactOption>();
        forEachBottomUp(this, (n) -> {
            if (n.hasArtifact()) {
                output.addAll(n.artifacts);
            }
        });
        return output;
    }

    @JsonIgnore
    public List<String> fetchCondition() {
        var output = new LinkedList<String>();
        forEachBottomUp(this, n -> {
            if (n.hasCondition()) {
                output.addFirst(n.condition);
            }
        });
        return output;
    }

    private static void forEachBottomUp(StepNode step, Consumer<StepNode> onNode) {
        onNode.accept(step);

        if (!step.hasParent()) {
            return;
        }

        if (step.getParent() instanceof StepNode p) {
            forEachBottomUp(p, onNode);
        }
    }

    private static void forEachBottomUp(StepNode step, Function<StepNode, Boolean> onNode) {
        if (!onNode.apply(step)) {
            return;
        }

        if (!step.hasParent()) {
            return;
        }

        if (step.getParent() instanceof StepNode p) {
            forEachBottomUp(p, onNode);
        }
    }
}
