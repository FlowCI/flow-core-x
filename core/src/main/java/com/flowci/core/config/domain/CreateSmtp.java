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
    private String name;

    @NotEmpty
    private String server;

    @NonNull
    private Integer port;

    @NonNull
    private Boolean isSecure;

    private String username;

    private String password;

    private String secret;

    public SmtpConfig toConfig() {
        SmtpConfig smtpConfig = new SmtpConfig()
                .setServer(server)
                .setPort(port)
                .setSecret(secret)
                .setIsSecure(isSecure)
                .setAuth(SimpleAuthPair.of(username, password));

        smtpConfig.setName(name);
        return smtpConfig;
    }
}
