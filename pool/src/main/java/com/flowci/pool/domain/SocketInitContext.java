package com.flowci.pool.domain;

import com.flowci.pool.DockerClientExecutor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocketInitContext extends InitContext {

    private DockerClientExecutor executor;
}