package com.flowci.parser.v2.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class StepYml extends NodeYml {

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

    private Integer retry; // num of retry

    private Integer timeout; // timeout in seconds

    private Boolean allow_failure;

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
}
