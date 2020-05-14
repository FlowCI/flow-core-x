package com.flowci.core.task;

import com.flowci.domain.DockerOption;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalDockerTask extends LocalTask {

    private DockerOption docker;

}
