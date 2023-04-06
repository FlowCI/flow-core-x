package com.flowci.parser.v2.yml;

import com.flowci.domain.StringVars;
import com.flowci.domain.tree.NodePath;
import com.flowci.domain.tree.StepNode;
import com.flowci.exception.YmlException;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class StepYml extends NodeYml implements Convertable<StepNode> {

    /**
     * Bash script
     */
    private String bash;

    /**
     * Powershell script
     */
    private String pwsh;

    private boolean post;

    private String plugin;

    private Integer retry = 0; // num of retry

    private Integer timeout = 0; // timeout in seconds

    private Boolean allow_failure = Boolean.FALSE;

    /**
     * Step dependencies, only first level steps allow to have
     * child step of step not allowed in dependencies
     */
    private List<String> dependencies = new LinkedList<>();

    private List<String> exports = new LinkedList<>();

    private List<String> secrets = new LinkedList<>();

    private List<String> configs = new LinkedList<>();

    private List<FileOptionYml> caches = new LinkedList<>();

    private List<FileOptionYml> artifacts = new LinkedList<>();

    @Override
    public StepNode convert() {
        return StepNode.builder()
                .vars(new StringVars(vars))
                .condition(condition)
                .dockers(dockers.stream().map(DockerOptionYml::convert).toList())
                .agents(agents)
                .bash(bash)
                .pwsh(pwsh)
                .post(post)
                .plugin(plugin)
                .retry(retry == null ? 0 : retry)
                .timeout(timeout == null ? 0 : timeout)
                .allowFailure(allow_failure != null)
                .dependencies(dependencies)
                .exports(new HashSet<>(exports))
                .secrets(new HashSet<>(secrets))
                .configs(new HashSet<>(configs))
                .caches(new HashSet<>(caches.stream().map(FileOptionYml::convert).toList()))
                .artifacts(new HashSet<>(artifacts.stream().map(FileOptionYml::convert).toList()))
                .steps(toStepNodeList())
                .build();
    }
}
