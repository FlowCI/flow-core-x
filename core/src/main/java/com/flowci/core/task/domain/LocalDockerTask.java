package com.flowci.core.task.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalDockerTask extends LocalTask {

    private String image;

}
