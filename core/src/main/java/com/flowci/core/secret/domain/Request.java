package com.flowci.core.secret.domain;

import com.flowci.common.domain.SimpleAuthPair;
import com.flowci.common.domain.SimpleKeyPair;
import com.google.common.base.Strings;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

public abstract class Request {

    @Data
    public abstract static class Base {

        @NotEmpty
        private String name;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CreateAuth extends Base {

        @NotEmpty
        private String username;

        @NotEmpty
        private String password;

        public SimpleAuthPair getAuthPair() {
            return SimpleAuthPair.of(username, password);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CreateRSA extends Base {

        @NotEmpty
        private String publicKey;

        @NotEmpty
        private String privateKey;

        public boolean hasKeyPair() {
            return !Strings.isNullOrEmpty(publicKey) && !Strings.isNullOrEmpty(privateKey);
        }

        public SimpleKeyPair getKeyPair() {
            return SimpleKeyPair.of(publicKey, privateKey);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CreateToken extends Base {

        @NotEmpty
        public String token;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CreateKubeConfig extends Base {

        @NotEmpty
        public String content;
    }
}
