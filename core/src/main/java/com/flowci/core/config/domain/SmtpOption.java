package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public final class SmtpOption {

    @NotEmpty
    private String server;

    @NonNull
    private Integer port;

    @NonNull
    private Boolean isSecure;

    private SimpleAuthPair auth;

    private String secret;
}
