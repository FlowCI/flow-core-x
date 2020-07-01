package com.flowci.core.config.domain;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public final class AndroidSignOption {

    @NotEmpty
    private String keyStorePassword;

    @NotEmpty
    private String keyAlias;

    @NotEmpty
    private String keyPassword;
}
