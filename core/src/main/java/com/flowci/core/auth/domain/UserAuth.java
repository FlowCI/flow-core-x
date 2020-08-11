package com.flowci.core.auth.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document("user_auth")
public final class UserAuth extends Mongoable {

    @Indexed(unique = true, name = "index_auth_email")
    private String email;

    @Indexed(unique = true, name = "index_auth_token")
    private String token;

    private String refreshToken;
}
