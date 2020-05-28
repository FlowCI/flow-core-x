package com.flowci.core.secret.domain;

import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.google.common.base.Strings;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

public abstract class Request {

    @Data
    public static class CreateAuth {

        @NotEmpty
        private String name;

        @NotEmpty
        private String username;

        @NotEmpty
        private String password;

        public SimpleAuthPair getAuthPair() {
            return SimpleAuthPair.of(username, password);
        }
    }

    @Data
    public class CreateRSA {

        @NotEmpty
        private String name;

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
}
