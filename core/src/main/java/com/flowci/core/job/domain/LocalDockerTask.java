package com.flowci.core.job.domain;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class LocalDockerTask extends LocalTask {

    private String image = "flowci/plugin-runtime";

    private Path pluginDir;
}
