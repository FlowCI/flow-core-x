package com.flowci.docker.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor(staticName = "of")
public final class SSHOption {

    /**
     * Private rsa key for remote host access
     */
    private final String privateKey;

    private final String remoteHost;

    private final String remoteUser;

    private final int port = 22;

    @Setter
    private int timeoutInSeconds = 10;
}
