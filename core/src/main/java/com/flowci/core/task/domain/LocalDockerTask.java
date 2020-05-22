package com.flowci.core.task.domain;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class LocalDockerTask extends LocalTask {

    private String image;

    private Path pluginDir;
}
