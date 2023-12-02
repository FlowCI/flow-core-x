package com.flowci.core.config.domain;

import com.flowci.common.helper.StringHelper;
import com.flowci.domain.SimpleAuthPair;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static com.flowci.core.config.domain.SmtpConfig.SecureType;

@Getter
@Setter
public final class SmtpOption {

    @NotEmpty
    private String server;

    @NonNull
    private Integer port;

    @NonNull
    private SecureType secure = SecureType.NONE;

    private SimpleAuthPair auth;

    private String secret;

    public boolean hasSecret() {
        return StringHelper.hasValue(secret);
    }
}
