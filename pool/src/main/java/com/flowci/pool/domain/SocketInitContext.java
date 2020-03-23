package com.flowci.pool.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocketInitContext extends InitContext {

    private String dockerHost = "unix:///var/run/docker.sock";

}