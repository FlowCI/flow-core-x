package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public final class CreateSmtp {

    @NotEmpty
    private String server;

    @NonNull
    private Integer port;

    private String username;

    private String password;

    private String secret;

    public SmtpConfig toConfig() {
        return new SmtpConfig()
                .setServer(server)
                .setPort(port)
                .setSecret(secret)
                .setAuth(SimpleAuthPair.of(username, password));
    }
}
