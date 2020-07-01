package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

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

    private String username;

    private String password;

    private String secret;

    public boolean hasSecret() {
        return StringHelper.hasValue(secret);
    }

    public SimpleAuthPair getAuthPair() {
        return SimpleAuthPair.of(username, password);
    }
}
