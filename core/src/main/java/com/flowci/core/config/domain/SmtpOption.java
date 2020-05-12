package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public final class SmtpOption {

    public enum SecureType {
        NONE,

        SSL,

        TLS,
    }

    @NotEmpty
    private String server;

    @NonNull
    private Integer port;

    @NonNull
    private SecureType secure = SecureType.NONE;

    private SimpleAuthPair auth;

    private String secret;
}
