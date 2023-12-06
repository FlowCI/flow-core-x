package com.flowci.core.secret.domain;

import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

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
