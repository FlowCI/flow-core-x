package com.flowci.pool.domain;

import com.github.dockerjava.api.DockerClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocketInitContext extends InitContext {

    private DockerClient client;

}